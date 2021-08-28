import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.*

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

        extend {
            val circle = Circle(w/2, h/2, Math.min(w, h)/2.5).contour

            drawer.clear(ColorRGBa.PINK)
            drawer.strokeWeight = 2.0

            for (i in -300 until height+300 step 10) {
                val ls = contour {
                    moveTo(0.0, i.toDouble())
                    curveTo(w/2, i.toDouble() + 300.0, w, i.toDouble())
                }
                val b = intersection(ls, circle)
                for (a in b.contours) {
                    val end = map(i * 0.2, (i+50)*0.2, 0.0, 1.0, seconds*10.0-3.0, true)
                    val start = map((i+600)*0.2, (i+650)*0.2, 0.0, 1.0, seconds*10.0-3.0, true)
                    drawer.contour(a.sub(start, end))
                }
            }
        }
    }
}