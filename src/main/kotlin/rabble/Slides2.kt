import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.draw.tint
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriterProfile
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.drawComposition
import kotlin.math.cos
import kotlin.math.sin

/**
 * See https://docs.google.com/presentation/d/e/2PACX-1vSfNYCVdqYbjYFvJh6iEc9ZlW6szCEbOtJ_Rt_iieDvTLAzLayo4foE6VCLiMcP973hTrfJXHmDTHdN/pub?start=false&loop=false&delayms=3000#slide=id.ga03caeeb6d_0_103
 * I copied the code from there
 */

fun main() = applicationSynchronous {
    configure {
        width = 1000
        height = 1000
        position = IntVector2(1400, 200)
    }

    program {
        val w = width.toDouble()
        val h = height.toDouble()

        val comp = drawComposition {
            circle(w/2, h/2, Math.min(w, h)/2.5)
            for (i in 0 until 10000) {
                val r = Rectangle(0.0, 0.0, w, h).random()
//                println(r)
                nearest(r)?.let {
                    lineSegment(r, it.point.position)
                }
//                println(nearest(r))
            }
        }

        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.stroke = ColorRGBa.BLACK
            drawer.strokeWeight = 3.0
            drawer.composition(comp)
        }
    }
}

private fun Rectangle.random() : Vector2 {
    val x = Math.random() * width + this.x
    val y = Math.random() * height + this.y
    return Vector2(x, y)
}