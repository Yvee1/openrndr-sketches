package current

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.extra.shapes.drawers.bezierPatch
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.sphereMesh
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment3D

fun main() = application {
    configure {
        width = 800
        height = 800
        windowResizable = true
    }

    program {
        fun pos(u: Double, v: Double, w: Double = 0.0): Vector3{
            val v = drawer.bounds.position(u, v) - drawer.bounds.center
            return Vector3(v.x, w * width, v.y)
        }

        // Straight
//        val c0 = LineSegment(pos(0.1, 0.1), pos(0.9, 0.1))
//        val c1 = LineSegment(pos(0.1, 0.334), pos(0.9, 0.334))
//        val c2 = LineSegment(pos(0.1, 0.567), pos(0.9, 0.567))
//        val c3 = LineSegment(pos(0.1, 0.9), pos(0.9, 0.9))

        // Curvy
        val c0 = Segment3D(pos(0.1, 0.1, 0.5), pos(0.5, 0.1, 0.3), pos(0.9, 0.1))
        val c1 = Segment3D(pos(0.4, 0.3, 0.3), pos(0.5, 0.4),pos(0.6, 0.4))
        val c2 = Segment3D(pos(0.4, 0.7), pos(0.5, 0.65),pos(0.6, 0.6))
        val c3 = Segment3D(pos(0.1, 0.9), pos(0.5, 0.9),pos(0.9, 0.9))

//        println(c0.path.segments[0].control.size)
        val bp = bezierPatch(c0, c1, c2, c3)
//        println(bp.position(0.0, 0.0))

        val sphere = sphereMesh(64, 64, 0.499)
        extend(Orbital())
//        extend(ScreenRecorder()){
//            multisample = BufferMultisample.SampleCount(8)
//        }
        extend {
            drawer.apply {
                clear(ColorRGBa.BLUE)
                stroke = ColorRGBa.BLACK

                for (i in 0..50) {
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.path(bp.horizontal(i / 50.0))
                    drawer.path(bp.vertical(i / 50.0))
                }

                fill = ColorRGBa.PINK
                bezierPatch(bp)

                isolated {
                    translate(0.0, 0.5, 0.0)
                    vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}