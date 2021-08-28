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
import org.openrndr.shape.*
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
        }

        val bounds = Rectangle(0.0, 0.0, w, h)

        for (i in 0 until 400) {
            drawComposition(composition = comp) {
                val tries = if (i < 100) 100 else 1
                val fn: Pair<Vector2, ShapeNodeNearestContour>? = farthestNearest(tries) { bounds.random() }
                fn?.let {
                    val q = it.first
                    val n = it.second
                    val direction = n.point.position - q
                    val sign = if (Math.random() < 0.5) -1.0 else 1.0
                    val c0 = direction.rotate(sign * 45.0) * 2.0
                    val r0 = LineSegment(q, q + c0).contour
                    intersections(r0, n.point.contour).firstOrNull()?.let {
                        n.node.terminal = false // <- what is this?
                        contour(contour {
                            moveTo(q)
                            curveTo(n.point.position, it.position)
                        }.reversed)
                    }
                }
            }
        }
        
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.composition(comp)
        }
    }
}

var CompositionNode.terminal: Boolean by UserData("terminal", true)

private fun CompositionDrawer.farthestNearest(tries: Int, function: () -> Vector2): Pair<Vector2, ShapeNodeNearestContour>? {
    val x = function()
    val n = nearest(x)
    return n?.let {Pair(x, it)}
}


private fun Rectangle.random() : Vector2 {
    val x = Math.random() * width + this.x
    val y = Math.random() * height + this.y
    return Vector2(x, y)
}