import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSLa
import org.openrndr.draw.Drawer
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.Reloadable
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.*
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import org.openrndr.shape.contours
import org.openrndr.shape.intersections
import org.openrndr.Fullscreen
import kotlin.math.abs
import kotlin.math.max

/**
 *  Made using the following tutorial: https://varun.ca/torsions/
 */

fun main() = application {
    configure {
//        width = 1000
//        height = 1000
        width = 2560
        height = 1440
        fullscreen = Fullscreen.SET_DISPLAY_MODE
    }

    oliveProgram {
        val gui = GUI()
        val settings = @Description("User settings") object : Reloadable() {
            @DoubleParameter("Wrapped amount", 0.0, 1.0)
            var wrappedAmount = 0.0
        }

        class Slice(val pos: Vector2, val w: Double, val h: Double, var wrappedAmount: Double = 0.0) : Animatable() {
//            var wrappedAmount = settings.wrappedAmount
            var bottomRight = pos + Vector2(w/2 , 0.0)
            val topRight = pos + Vector2(w/2, -h)


            fun draw(drawer: Drawer){
                val offset = (w/4) * (0.5 - abs(wrappedAmount - 0.5))

                val x1 = bottomRight.x - wrappedAmount * w
                val y1 = bottomRight.y
                val p1 = Vector2(x1, y1)
                val p1_ = Vector2(x1 - offset, y1)
                val x2 = topRight.x
                val y2 = topRight.y - (1 - wrappedAmount) * (topRight.y - bottomRight.y)
                val p2 = Vector2(x2, y2)

                if (wrappedAmount > 0.001) {
                    val K1  =  max(2*(wrappedAmount - 0.5)*0.37, 0.0)
                    val K1_ = max(2*(wrappedAmount - 0.5)*0.6, 0.0)
                    val K2  = 0.37

                    val c1  = smoothCurveBetween(p1, p2, K1, K2)
                    val c2  = smoothCurveBetween(reflectX(p1), reflectX(p2), K1, K2)
                    val c2_ = smoothCurveBetween(reflectX(p1_), reflectX(p2), K1_, K2)

                    val cp2 = Vector2(x2, y2 - K2 * (y2 - y1))

                    val inters1 = intersections(c1, c2)
                    val inters2 = intersections(c2_, c1)

                    // Draw the back
                    if (inters1.isNotEmpty()){
                        val piece1 = c1.sub(0.0,inters1[0].a.contourT)
                        val piece2 = c2.sub(0.0, inters1[0].b.contourT)
                        val piece3 = contour{
                            moveTo(p1)
                            lineTo(reflectX(p1))
                            close()
                        }
                        val whole = ShapeContour((piece1 + piece2.reversed + piece3).segments, closed=true)

                        // purplish
                        drawer.fill = ColorXSLa(280.0, 0.6, 0.8, 1.0).toRGBa()
                        drawer.contour(whole)
                    }

                    // Draw the front
                    if (inters2.isNotEmpty()){
                        val inter = inters2[0].position;

                        val piece1 = c1.sub(inters2[0].b.contourT, 1.0)
                        val piece2 = c2_.sub(inters2[0].a.contourT, 1.0)
                        val piece3 = contour{
                            moveTo(reflectX(topRight))
                            lineTo(topRight)
                            lineTo(p2)
                        }
                        val whole = ShapeContour((piece1.reversed + piece2 + piece3).segments, closed=true)

                        // yellowish
                        drawer.fill = ColorXSLa(110.0, 0.6, 0.8, 1.0).toRGBa()
                        drawer.contour(whole)
                    }

                    // Draw the front when there is no intersection
                    if (inters2.isEmpty() && inters1.isEmpty()){
                        val front = contour {
                            moveTo(topRight)
                            lineTo(p2)
                            curveTo(cp2, p1)
                            lineTo(reflectX(p1_))
                            curveTo(reflectX(cp2), reflectX(p2))
                            lineTo(reflectX(topRight))
                            lineTo(topRight)
                            close()
                        }

                        drawer.fill = ColorXSLa(110.0, 0.6, 0.8, 1.0).toRGBa()
                        drawer.contour(front)
                    }

                    // Draw the thick white edge
                    drawer.fill = ColorRGBa.WHITE

                    val connector = contour {
                        moveTo(reflectX(p1_))
                        lineTo(reflectX(p1))
                    }

                    val whiteCurve = ShapeContour((c2_.reversed + connector + c2).segments, closed=true)
                    drawer.contour(whiteCurve)
                }

                // Crashes when we want to draw a line between points that are at the same location
                // so we have seperate case for wrappedAmount = 0
                else {
                    val front = contour {
                        moveTo(p1)
                        lineTo(reflectX(p1))
                        lineTo(reflectX(topRight))
                        lineTo(topRight)
                        close()
                    }

                    drawer.fill = ColorXSLa(110.0, 0.6, 0.8, 1.0).toRGBa()
                    drawer.contour(front)
                }

            }

            fun smoothCurveBetween(x1: Double, y1: Double, x2: Double, y2: Double, K1: Double, K2: Double): ShapeContour {
                val cp1 = Vector2(x1, y1 + K1 * (y2 - y1))
                val cp2 = Vector2(x2, y2 - K2 * (y2 - y1))

                return contour {
                    moveTo(x1, y1)
                    curveTo(cp1, cp2, Vector2(x2, y2))
                }
            }

            fun smoothCurveBetween(p1: Vector2, p2: Vector2, K1: Double, K2: Double): ShapeContour {
                return smoothCurveBetween(p1.x, p1.y, p2.x, p2.y, K1, K2)
            }

            fun reflectX(v: Vector2): Vector2 {
                return Vector2(pos.x - (v.x - pos.x), v.y)
            }
        }

        val w = 0.07*width
        val h = 0.4*height
        val pos = Vector2(width.toDouble()/2.0, height-(height-h)/2)
        val mainSlice = Slice(pos, w, h)
        val N = 55
        val slices = Array(N) { i -> Slice( Vector2(50.0*i, height-(height-h)/2), w, h) }
//        val asc = Array(5) { i -> Slice(pos + Vector2(50.0, 0.0), w, h) }
//        extend(gui) {
//            add(settings)
//        }

        var test = 0

//        extend(ScreenRecorder())
        extend {
            drawer.apply {
                clear(ColorRGBa(0.9, 0.9, 0.93))
                fill = ColorXSLa(110.0, 0.6, 0.8, 1.0).toRGBa()
                stroke = ColorRGBa.BLACK
            }

            val slice = mainSlice

//            for (i in slices.indices) {
//                val slice = slices[i]
            slice.updateAnimation()
////                settings.wrappedAmount = slice.wrappedAmount
//
            slice.draw(drawer)
            val duration = 7
//
            if (!slice.hasAnimations()) {
                if (test == 1) {
                    application.exit()
                }
                slice.apply {
//                        delay(i.toLong()*50)
//                    animate("wrappedAmount", 0.999, (duration * 1000 / 2).toLong(), Easing.QuadInOut)
//                    complete()
//                    animate("wrappedAmount", 0.0, (duration * 1000 / 2).toLong(), Easing.QuadInOut)
//                    complete()
                }
                //                test++
            }
//            }
        }
    }
}