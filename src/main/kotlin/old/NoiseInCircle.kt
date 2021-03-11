import org.openrndr.application
import org.openrndr.color.ColorRGBa
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
import kotlin.math.sin

/**
 * See https://necessarydisorder.wordpress.com/2017/11/15/drawing-from-noise-and-then-making-animated-loopy-gifs-from-there/
 */

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        val RECORD = false

        val seed = 0
        val scale = 0.005
        val r = 1.2
        val duration = 4.5

        val cr = 0.4*width

        val points = poissonDiskSampling(cr*2, cr*2, 3.0, 50, true) { w: Double, h: Double, v: Vector2 ->
            Circle(Vector2(w, h) / 2.0, cr).contains(v)
        }

        val circlePoints = points.map { Circle(Vector2(width/2.0 - cr, height/2.0 - cr) + it, 1.4) }

        if (RECORD) {
            extend(ScreenRecorder()) {
                maximumDuration = duration
                profile = GIFProfile()
                frameRate = 50
            }
            extend(TemporalBlur()) {
                fps = 50.0
                this.duration = 0.8
            }
        }
        extend {
            val newPoints = circlePoints.map {
                val dx = simplex(seed, it.center.x*scale, it.center.y*scale, r*cos(seconds/duration*2*PI), r*sin(seconds%duration/duration*2*PI))
                val dy = simplex(seed+1, it.center.x*scale, it.center.y*scale, r*cos(seconds/duration*2*PI), r*sin(seconds%duration/duration*2*PI))
                val a = map(0.0, cr*cr, 200.0, 0.0, (it.center - Vector2(width/2.0, height/2.0)).squaredLength)
                it.moved(Vector2(a*dx, a*dy))
            }
            drawer.apply {
                val color = ColorRGBa(1.0, 1.0, 1.0, 0.7)
                fill = color
                stroke = color
                circles(newPoints)
            }
        }
    }
}