import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.olive.oliveProgram
import java.io.File
import kotlin.math.*

fun main() = applicationSynchronous {
    configure {
        width = 1000
        height = 1000
    }

    oliveProgram {
        val newWidth = width
        val newHeight = 350

        print(newWidth)

        val rt = renderTarget(newWidth, newHeight) {
            colorBuffer()
        }

        drawer.isolatedWithTarget(rt){
            clear(ColorRGBa.BLACK)
            fill = ColorRGBa.PINK
            rectangle(newWidth/2.0, newHeight.toDouble(), 100.0, 100.0)
        }

        extend(Screenshots())
        extend {
            drawer.image(rt.colorBuffer(0))

            drawer.fill = ColorRGBa.BLUE
            drawer.stroke = null
            drawer.rectangle(newWidth/2.0, newHeight.toDouble(), 100.0, 100.0)
        }

        rt.colorBuffer(0).saveToFile(File("test.png"))
    }
}