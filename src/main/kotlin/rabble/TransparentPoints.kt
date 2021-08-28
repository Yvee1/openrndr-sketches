import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.olive.oliveProgram

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        extend(Screenshots())
        extend {
            drawer.apply {
                clear(ColorRGBa.BLACK)
                stroke = null
                fill = ColorRGBa.WHITE
                point(width/2.0, height/2.0)
                circle(width/2.0 + 10, height/2.0, 2.5)
            }
        }
    }
}