import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.fastFloor
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mod
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * See https://necessarydisorder.wordpress.com/2017/11/15/drawing-from-noise-and-then-making-animated-loopy-gifs-from-there/
 */

fun main() = applicationSynchronous {
    configure {
        width = 500
        height = 500
    }

    program {
        val RECORD = true

        val seed = 0
        val scale = 0.005
        val r = 1.2
        val duration = 0.75

        val dt = 2.0

        data class Particle(val pos: Vector2, val alpha: Double)

        class Path {
            val positions = mutableListOf<Vector2>()
            var current = Vector2.uniform(drawer.bounds)
            val tOff = Double.uniform(0.0, 1.0)
            val nParticles = 40

            init {
                positions.add(current)
            }

            fun field(pos: Vector2): Vector2 {
                return Vector2(0.0, 1.0)
            }

            fun update(){
                current += field(current) * dt
                positions.add(current)
            }

            fun getPoints(time: Double): List<Particle>{
                val t = mod((time / duration + tOff), 1.0)
//                println(t)
                return List(nParticles) {
                    val gIndex = map(0.0, nParticles.toDouble(), 0.0, positions.size-1.001, t+it)
                    val i = gIndex.fastFloor()
                    val j = i + 1
                    val s = gIndex - i
                    val pos = positions[i]*(1-s) + positions[j]*s
                    val alpha = gIndex / positions.size
                    Particle(pos, alpha)
                }
            }
        }

        val paths = List(3000) {Path()}
        paths.forEach {
            for (i in 0 until 500){
                it.update()
            }
        }

        if (RECORD) {
            extend(ScreenRecorder()) {
                maximumDuration = duration
                profile = GIFProfile()
                frameRate = 50
            }
            extend(TemporalBlur()) {
                fps = 50.0
                samples = 30
                this.duration = 3.0
                jitter = 0.05
            }
        }
        extend {
            val pts = paths.map {
                it.getPoints(seconds)
            }.flatten()
            drawer.apply{
                clear(ColorRGBa.BLACK)
                val rect = Rectangle.fromCenter(Vector2(width/2.0+0.5, height/2.0+0.5), width*0.8)
                fill = null
                stroke = ColorRGBa.WHITE
                strokeWeight = 0.5
                contour(rect.contour)
                drawStyle.clip = Rectangle.fromCenter(Vector2(width/2.0, height/2.0), width*0.8)
                fill = ColorRGBa.WHITE
//                stroke = null
                circles(pts.map{it.pos}, pts.map{it.alpha+1.1})
            }
        }
    }
}