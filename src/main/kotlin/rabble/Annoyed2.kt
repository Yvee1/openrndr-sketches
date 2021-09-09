package current

import org.openrndr.PresentationMode
import org.openrndr.application
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.drawComposition
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.shape.LineSegment
import org.openrndr.svg.loadSVG
import kotlin.concurrent.thread

fun main() {
    application {
        program {
            val con = LineSegment(0.0, 0.0, width*1.0, height*1.0).contour
            val svg = loadSVG("Via_Flaminia_map.svg")
            val shapes = svg.findShapes()
            val road = shapes.first().shape.contours[0]
            shapes.forEach { it.stroke = ColorRGBa.BLACK }
            shapes.forEach { it.fill = ColorRGBa.WHITE }
//            println(svg.calculateViewportTransform())
//            println(shapes.last())
//            println(road)
            extend {
                drawer.apply {
                    translate(0.0, -1000.0)
                    clear(ColorRGBa.PINK)
                    composition(svg)
//                    contour(road)
                }
            }
        }
    }
}