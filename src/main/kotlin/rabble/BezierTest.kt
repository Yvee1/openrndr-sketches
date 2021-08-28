package current

import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.extra.shapes.drawers.bezierPatch
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.LineSegment
import org.openrndr.shape.contour

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }

    program {
        fun pos(u: Double, v: Double) = drawer.bounds.position(u, v)

        // Straight
//        val c0 = LineSegment(pos(0.1, 0.1), pos(0.9, 0.1))
//        val c1 = LineSegment(pos(0.1, 0.334), pos(0.9, 0.334))
//        val c2 = LineSegment(pos(0.1, 0.567), pos(0.9, 0.567))
//        val c3 = LineSegment(pos(0.1, 0.9), pos(0.9, 0.9))

        // Curvy
        val c0 = LineSegment(pos(0.1, 0.1), pos(0.9, 0.1))
        val c1 = LineSegment(pos(0.4, 0.3), pos(0.6, 0.4))
        val c2 = LineSegment(pos(0.4, 0.7), pos(0.6, 0.6))
        val c3 = LineSegment(pos(0.1, 0.9), pos(0.9, 0.9))

        val bp = bezierPatch(c0.segment, c1.segment, c2.segment, c3.segment)

        extend {
            drawer.apply {
                clear(ColorRGBa.BLUE)
                stroke = ColorRGBa.BLACK
                fill = ColorRGBa.PINK
                this.bezierPatch(bp)
                for (i in 0..50) {
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.contour(bp.horizontal(i / 50.0))
                    drawer.contour(bp.vertical(i / 50.0))
                }
            }
        }
    }
}