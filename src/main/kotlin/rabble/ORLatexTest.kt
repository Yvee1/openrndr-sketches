import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.shape.GroupNode
import org.openrndr.shape.ShapeNode
import org.openrndr.svg.loadSVG
import useful.LatexText
import kotlin.math.log

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
//        toSVG("\\delta", "temp.svg", true)
//        val svg = loadSVG("target/temp.svg")
        val latexText = LatexText("\\delta", 10.0f)
        val svg = latexText.composition
//        val shape = svg.findShapes()[0]
//        println(svg.findGroups().map {it.attributes})
//        println(svg.root.bounds)
//        println(svg.findShapes())
//        svg.findShapes()[0].stroke = ColorRGBa.BLACK
        val myNode = (((svg.root as GroupNode).children[1] as GroupNode).children[0] as GroupNode).children[0] as ShapeNode
//        myNode.fill = ColorRGBa.BLACK
//        myNode.stroke = ColorRGBa.BLACK
        val myShape = myNode.shape
//        println(svg.findShapes()[0].shape.)


        extend {
            drawer.isolated {
                clear(ColorRGBa.WHITE)
                stroke = ColorRGBa.BLACK
//                lineSegment(Vector2(width / 2.0, 0.0), Vector2(width / 2.0, height * 1.0))
//                lineSegment(Vector2(0.0, height / 2.0), Vector2(width * 1.0, height / 2.0))
                scale(mouse.position.x / 400.0 + 0.1)
                isolated {
//                    translate(width/4.0, 0.0)
//                    scale(10.0, 10.0)
                    composition(svg)
                }

//                translate(width / 2.0, height / 2.0)
                translate(width/2.0, 80.0)
//                scale(0.2, 0.2)
                fill = ColorRGBa.BLACK
                stroke = null
//                shape(svg.findShapes()[0].shape)
                shape(myShape)
            }
            drawer.isolated {
//                scale(20.0, 20.0)
//                shape(svg.findShapes()[1].effectiveShape)

//            drawer.scale(10.0, 10.0)
//            println(svg.findShapes()[1].effectiveTransform)

//                drawer.composition(svg.)
            }
        }
    }
}