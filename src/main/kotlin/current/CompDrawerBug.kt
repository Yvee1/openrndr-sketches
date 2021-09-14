package current

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.drawComposition
import org.openrndr.extensions.Screenshots
import org.openrndr.shape.contour
import org.openrndr.svg.saveToFile
import java.io.File

fun main() = application {
    program {
        val comp = drawComposition {
            fill = ColorRGBa.GREEN
            stroke = ColorRGBa.BLACK
            rectangle(width/2.0, height/2.0, 100.0, 100.0)
            contour(contour {
                moveTo(100.0, 100.0)
                lineTo(100.0, 300.0)
                lineTo(150.0, 200.0)
            })
            stroke = ColorRGBa.PINK
            lineSegment(150.0, 200.0, width/2.0, height/2.0)
        }

        comp.saveToFile(File("culprit.svg"))
        extend(Screenshots())
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(comp)
        }
    }
}