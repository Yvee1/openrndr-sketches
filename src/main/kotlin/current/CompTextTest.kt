package current

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.drawComposition
import org.openrndr.extensions.Screenshots
import org.openrndr.shape.Composition
import org.openrndr.svg.saveToFile
import useful.LatexText
import useful.text
import java.io.File

fun main() = application {
    program {
        val comps = mutableListOf<Composition>()
        comps.add(LatexText("$\\int_0^\\infty$", 14.0).composition)
        comps.add(drawComposition { rectangle(0.0, 0.0, 40.0, 40.0) })
        comps.add(drawComposition { stroke=null; fill=ColorRGBa.GREEN; rectangle(0.0, 0.0, 40.0, 40.0) })

        val nestedComps = comps.map { drawComposition { composition(it) } }

        extend(Screenshots())
        extend {
            drawer.apply {
                clear(ColorRGBa.WHITE)
                isolated {
                    comps.forEach {
                        drawer.composition(it)
                        drawer.translate(0.0, 50.0)
                    }
                }
                translate(200.0, 0.0)
                isolated {
                    nestedComps.forEach {
                        drawer.composition(it)
                        drawer.translate(0.0, 50.0)
                    }
                }
            }
        }
    }
}