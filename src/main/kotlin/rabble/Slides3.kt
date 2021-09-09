import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.drawComposition
import org.openrndr.svg.toSVG
import java.io.File
import kotlin.random.Random

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

        val comp = drawComposition {
            val ls = LineSegment(w/2 - 300.0, h/2, w/2 + 300.0, h/2)

            lineSegment(ls.rotate(seconds*18.0))
            val gen = Random(10)
            for (i in 0 until 400) {
                val r = Rectangle(0.0, 0.0, w, h).random(gen)
                val n = nearest(r)
                n?.let {
                    lineSegment(r, it.point.position)
                }
            }
        }
        val svg = comp.toSVG()
        File("test-slides.svg").writeText(svg)
        println(svg)

        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.stroke = ColorRGBa.BLACK
            drawer.strokeWeight = 3.0

            drawer.composition(comp)
        }
    }
}

private fun Rectangle.random(gen: Random) : Vector2 {
    val x = gen.nextDouble() * width + this.x
    val y = gen.nextDouble() * height + this.y
    return Vector2(x, y)
}