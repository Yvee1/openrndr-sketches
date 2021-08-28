import org.openrndr.Fullscreen
import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSVa
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.*
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.*
import kotlin.math.*

/**
 *  Copy of an early version of Annulus.kt
 */

fun main() = applicationSynchronous {
    val w = 1000
    val h = 1000

    configure {
        width = w
        height = h
        position = IntVector2(1300, 200)
//        fullscreen = Fullscreen.SET_DISPLAY_MODE
    }

//    var points = poissonDiskSampling(w.toDouble(), h.toDouble(), 50.0, 10)
    val points = List(500) { Vector2.uniform(Vector2.ZERO, Vector2(w.toDouble(), h.toDouble())) }
    val rectPoints = points.map { Circle(it, 6.0) }


    program {
        val center = Vector2(width/2.0, height/2.0)

        val gui = GUI()

        val s = object {
            @IntParameter("Segments", 5, 500)
            var segments: Int = 10

            @IntParameter("Stars", 0, 100)

            @DoubleParameter("Outer radius", 0.1, 500.0)
            var outerRadius: Double = 100.0

            @XYParameter("Origin", 0.0, 1000.0, 0.0, 1000.0, invertY=false)
            var origin: Vector2 = center.copy()

            @XYParameter("Center", 0.0, 1000.0, 0.0, 1000.0, invertY=false)
            var center: Vector2 = center.copy()
        }

        gui.add(s, "Settings")

        val comp = compose {
            layer {
                draw {
                    drawer.clear(ColorRGBa.BLACK)
                }
            }

            layer {
                layer {
                    draw {
                        drawer.stroke = null
                        drawer.fill = ColorRGBa.WHITE
                        drawer.circles(rectPoints)
                    }

                    post(GaussianBloom()).addTo(gui)
                }

                layer {
                    draw {
                        for (i in 0 until s.segments) {
                            // Current angle
                            val ca: Double = i / s.segments.toDouble() * PI * 2
                            // Next angle
                            val na: Double = (i + 1) / s.segments.toDouble() * PI * 2
                            // Inner angle

                            val tri = contour {
                                moveTo(s.center + Vector2(cos(ca), sin(ca)) * s.outerRadius)
                                lineTo(s.center + Vector2(cos(na), sin(na)) * s.outerRadius)
                                lineTo(s.origin)
                                close()
                            }

                            drawer.fill = ColorXSVa(i.toDouble() / s.segments * 360.0, 0.7, 1.0, 1.0).toRGBa()
                            drawer.contour(tri)
                        }
                    }
                }

            }
        }

        extend(gui)
        extend {
            comp.draw(drawer)
        }

    }
}