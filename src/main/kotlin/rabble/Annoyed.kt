package current

import org.openrndr.PresentationMode
import org.openrndr.application
import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.svg.loadSVG
import kotlin.concurrent.thread

fun main() {
    applicationSynchronous {
        program {
            val comp = loadSVG("culprit.svg")
            val settings = object {
                val test = 10.0
            }

            extend {
                drawer.apply {
                    settings.test
                    clear(ColorRGBa.WHITE)
                    translate(width / 2.0 - 100.0, height / 2.0)
                    composition(comp)
                    translate(100.0, 0.0)
                    comp.findShapes().map { it.stroke = ColorRGBa.BLACK }
                    composition(comp)
                }
            }
        }
    }
}