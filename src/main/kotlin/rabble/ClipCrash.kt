import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.shadow.DropShadow
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.shape.ClipMode
import org.openrndr.shape.drawComposition

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        val center = Vector2(width/2.0, height/2.0)
        val c = compose {
            layer {
                draw {
                    drawer.clear(ColorRGBa.PINK)
                }
            }

            layer {
                draw {
                    drawer.composition(drawComposition {
                        fill = ColorRGBa.WHITE
                        stroke = null
                        clipMode = ClipMode.DISABLED
                        for (i in 0 until 5) {
                            circle(center, 200.0)
                        }
                        clipMode = ClipMode.DIFFERENCE

                        circle(center, 100.0)
                    })
                }
                post(DropShadow())
            }
        }

        extend {
            c.draw(drawer)
        }
    }
}