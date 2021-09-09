package current

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Circle

fun main() = application {
    program {
        extend {
            val c1 = Circle(width/3.0, height/2.0, width/3.0)
            val c2 = Circle(2*width/3.0, height/2.0, width/3.0)

            drawer.apply {
                stroke = null
                fill = ColorRGBa.WHITE.opacify(0.5)
                circles(listOf(c1, c2))
            }
        }
    }
}