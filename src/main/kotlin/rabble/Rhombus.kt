import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        val duration = 4.0
        val alpha = toRadians(45.0/2)

        fun drawRhombus(drawer: Drawer, time: Double, a: Double, n: Int){
            val hh = a * cos(alpha)
            val hw = a * sin(alpha)
            val rhombus = contour {
                moveTo(0.0, hh)
                lineTo(hw, 0.0)
                lineTo(0.0, -hh)
                lineTo(-hw, 0.0)
                close()
            }
            drawer.apply {
                contour(rhombus)
                if (n == 0) return

                strokeWeight *= 0.5
                isolated {
                    translate(hw/2.0, 0.0)
                    drawRhombus(this, time, a/2, 0)
                }

                isolated {
                    translate(-hw / 2.0, 0.0)
                    drawRhombus(this, time, a / 2, n - 1)
                }
            }
        }

        extend {
            val t = seconds / duration % 1

            drawer.apply {
                clear(ColorRGBa.BLACK)

                isolated {
                    translate(drawer.bounds.center)
                    stroke = ColorRGBa.WHITE
                    strokeWeight = 3.0
                    fill = null
                    drawRhombus(this, t, 300.0, 6)
                }
            }
        }
    }
}