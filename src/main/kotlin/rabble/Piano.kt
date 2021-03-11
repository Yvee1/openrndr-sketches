import org.openrndr.application
import org.openrndr.color.ColorXSLa
import org.openrndr.color.ColorXSVa
import org.openrndr.extra.olive.oliveProgram

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        extend {
            drawer.clear(ColorXSVa(80.0, 0.7, 0.8, 1.0).toRGBa())
            drawer.circle(width/2.0, height/2.0, 10.0)
        }
    }
}