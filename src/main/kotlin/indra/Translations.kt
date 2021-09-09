package indra

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.DrawQuality
import org.openrndr.draw.isolated
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.groundPlaneMesh
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment3D
import useful.FPSDisplay
import useful.complexToSphere
import useful.toComplex

fun main() = application {
    configure {
        windowResizable = true
        multisample = WindowMultisample.SampleCount(8)
    }

    program {
        extend(Orbital())

        val s = object {
            @IntParameter("Point count", 1, 10000)
            var pointCount = 100

            @DoubleParameter("Rectangle width", 1.0, 10000.0)
            var w = 10.0

            @DoubleParameter("Translation", 0.0, 1.0)
            var t = 0.0
        }

        val gui = GUI()
        gui.add(s)

        extend(gui)
        extend(FPSDisplay())
        extend {
            val w = s.w
            val plane = groundPlaneMesh(w, w)
            val seg = Segment3D(Vector3(-0.75 + s.t*1.5, 0.0, 0.0), Vector3(0.5 + s.t*1.5, 0.0, 0.0))

            fun sampleNonLinear(a: Vector2, b: Vector2, t: Double): Vector2 {
                val dir = b.normalized
                return a + dir * (1 / (1 - t) - 1)
            }

//            val planePoints = Rectangle(-w-0.75, -w, w, 2*w).contour.equidistantPositions(s.pointCount)
//            val planePoints1 = LineSegment(Vector2(-0.75, -w), Vector2(-0.75, w)).contour.equidistantPositions(s.pointCount)
//            val planePoints2 = List(s.pointCount){ sampleNonLinear(Vector2(-0.75, w), Vector2(-1.0, 0.0), it.toDouble()/s.pointCount) }
//            val planePoints4 = List(s.pointCount){ sampleNonLinear(Vector2(-0.75, -w), Vector2(-1.0, 0.0), it.toDouble()/s.pointCount) }.reversed()
//            val planePoints3 = LineSegment(planePoints2.last(), planePoints4.first()).contour.equidistantPositions(s.pointCount)
//            val planePoints = planePoints3 //+ planePoints2 + planePoints3 + planePoints4

            val planePointsL = List(s.pointCount){sampleNonLinear(Vector2(-0.75 + 1.5*s.t, 0.0), Vector2(1.5*s.t, 1.0), it.toDouble()/s.pointCount)}.reversed() + List(s.pointCount){sampleNonLinear(Vector2(-0.75 + 1.5*s.t, 0.0), Vector2(1.5*s.t, -1.0), it.toDouble()/s.pointCount)}
            val planePointsR = List(s.pointCount){sampleNonLinear(Vector2(0.75 + 1.5*s.t, 0.0), Vector2(1.5*s.t, 1.0), it.toDouble()/s.pointCount)}.reversed() + List(s.pointCount){sampleNonLinear(Vector2(0.75 + 1.5*s.t, 0.0), Vector2(1.5*s.t, -1.0), it.toDouble()/s.pointCount)}

            val circleOnSphereL = planePointsL.map { complexToSphere(it.toComplex()) }
            val circleOnSphereR = planePointsR.map { complexToSphere(it.toComplex()) }
//            val segOnSphere = seg.

            drawer.apply {
                clear(ColorRGBa.WHITE)
                fill = ColorRGBa.RED

                isolated {
                    translate(-w/2 - 0.75 + s.t*1.5, 0.0, 0.0)
                    vertexBuffer(plane, DrawPrimitive.TRIANGLES)
                }
                isolated {
                    translate(w/2 + 0.75 + s.t*1.5, 0.0, 0.0)
                    vertexBuffer(plane, DrawPrimitive.TRIANGLES)
                }
                for (i in -5..5){
                    isolated {
                        translate(0.0, 0.0, i.toDouble())
                        segment(seg)
                    }
                }

                strokeWeight = 20.0
                lineLoop(circleOnSphereL + circleOnSphereL.first())
                lineLoop(circleOnSphereR + circleOnSphereR.first())
            }
        }
    }
}