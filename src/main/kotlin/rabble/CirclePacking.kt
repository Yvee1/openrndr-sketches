import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape
import org.openrndr.svg.loadSVG

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        fun packCircles(shape: Shape, r: Double): List<Circle> {
            val rect = shape.bounds
//            val points = poissonDiskSampling(rect.width, rect.height, r, 200) { _, _, v ->
//                shape.closedContours.any { (v + rect.corner) in it }
//            }.map { it+rect.corner }
//
//            return points.map { Circle(it, r/2) }.filter { circle ->
//                circle.contour.equidistantPositions(5).all { p -> shape.closedContours.any { p in it } }
//            }
            return emptyList()
        }

        val butterfly = loadSVG("data/images/butterfly.svg").findShapes()[0].effectiveShape.transform(Matrix44.scale(0.5, 0.5, 1.0)* Matrix44.translate(150.0, 350.0, 0.0))
        val butterflyCircles = packCircles(butterfly, 30.0)
        extend {
            drawer.apply {
                stroke = null
                strokeWeight = 0.0
                circles(butterflyCircles)
                fill = null
                stroke = ColorRGBa.WHITE
                shape(butterfly)
            }
        }
    }
}

//fun Shape.contains(v: Vector2){
//    closedContours.any { v in it }
//}