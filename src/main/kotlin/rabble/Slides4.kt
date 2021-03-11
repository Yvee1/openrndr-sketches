import org.openrndr.application
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
import org.openrndr.shape.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * See https://docs.google.com/presentation/d/e/2PACX-1vSfNYCVdqYbjYFvJh6iEc9ZlW6szCEbOtJ_Rt_iieDvTLAzLayo4foE6VCLiMcP973hTrfJXHmDTHdN/pub?start=false&loop=false&delayms=3000#slide=id.ga03caeeb6d_0_103
 * I copied the code from there
 */

fun main() = application {
    configure {
        width = 1000
        height = 1000
        position = IntVector2(1400, 200)
    }

    program {
        val w = width.toDouble()
        val h = height.toDouble()

        val comp = drawComposition {  }

        extend {
            drawer.clear(ColorRGBa.PINK)

            drawComposition(composition = comp) {
                val c = contour {
                    moveTo(Math.random() * w, Math.random() * h)
                    curveTo(Math.random() * w, Math.random() * h, Math.random() * w, Math.random() * w)
                }
                val ints = intersections(c)
                if (ints.isEmpty()) {
                    contour(c)
                } else {
                    val holes = Shape.compound(ints.map { Circle(it.intersection.position, 10.0).shape })
                    shape(difference(c, holes))
                }
            }

            drawer.composition(comp)
        }
    }
}

private fun Rectangle.random() : Vector2 {
    val x = Math.random() * width + this.x
    val y = Math.random() * height + this.y
    return Vector2(x, y)
}