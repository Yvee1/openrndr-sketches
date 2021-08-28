import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/**
 * See https://necessarydisorder.wordpress.com/2017/11/15/drawing-from-noise-and-then-making-animated-loopy-gifs-from-there/
 */

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        val RECORD = false

        val seed = 0
        val scale = 0.02
        val r = 0.25
        val duration = 2.5

        if (RECORD) {
            extend(ScreenRecorder()) {
                maximumDuration = duration
                profile = GIFProfile()
                frameRate = 30
            }
            extend(TemporalBlur()) {
                fps = 30.0
                this.duration = 0.8
            }
        }

        fun offset(v: Vector2): Vector2? {
            val dx = simplex(seed, v.x*scale, v.y*scale, r*cos(seconds/duration*2*PI), r*sin(seconds%duration/duration*2*PI))
            val dy = simplex(seed+1, v.x*scale, v.y*scale, r*cos(seconds/duration*2*PI), r*sin(seconds%duration/duration*2*PI))

            val off = Vector2(dx, dy)*2.0
            return if (off.length > 0) v + off else null
//            return v + off
        }

        val cr = 0.1*width

        val points = poissonDiskSampling(cr*2, cr*2, 7.0, 50, true) { w: Double, h: Double, v: Vector2 ->
            Circle(Vector2(w, h) / 2.0, cr).contains(v)
        }.map {
            it + Vector2(width/2.0 - cr, height/2.0 - cr)
        }.toMutableList()

        mouse.dragged.listen {
            points.add(mouse.position)
        }

        mouse.buttonDown.listen {
            points.add(mouse.position)
        }
//        val points = mutableListOf<Vector2>()
//        for (x in 100 until 700 step 40){
//            for (y in 100 until 700 step 40){
//                points.add(Vector2(x.toDouble(), y.toDouble()))
//            }
//        }

        val allPoints = mutableListOf<Vector2>()
        extend {
            allPoints += points

            var newPoints = points.toList()
            for (i in 0 until 200){
                newPoints = newPoints.mapNotNull { offset(it) }
                allPoints += newPoints
            }

            drawer.apply {
                stroke = null
                fill = ColorRGBa(1.0, 1.0, 1.0, 0.4)
                circles(allPoints, 2.0)
            }

            allPoints.clear()

//            drawer.apply {
//                fill = ColorRGBa.WHITE
//                points(points)
//            }
        }
    }
}