import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        extend(Screenshots())
        extend {
            drawer.apply {
                drawer.clear(ColorRGBa.PINK)
                stroke = ColorRGBa.BLACK
                strokeWeight = 0.5
                contour(Rectangle.fromCenter(Vector2(width/2.0+0.5, height/2.0+0.5), 100.0, 100.0).contour)
                contour(LineSegment(Vector2.ZERO, Vector2(width/2.0, height/2.0)).contour)
                contour(org.openrndr.shape.contour {
                    moveTo(100.0, 300.0)
                    curveTo(150.0, 350.0, 100.0, 400.0)
                    curveTo(50.0, 350.0, 100.0, 300.0)
                    close()
                })
                contour(LineSegment(Vector2(0.0, 700.0), Vector2(width*1.0, 700.0)).contour)
                rectangle(Rectangle.fromCenter(Vector2(width/2.0+150.0, height/2.0), 100.0, 100.0))
            }
        }
    }
}