import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.patterns.Checkers
import kotlin.math.PI
import kotlin.math.sin
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }
    program {
        val rt = renderTarget(width, height) {
            colorBuffer()
        }

        val c = compose {
            layer {
                post(Checkers()) {
                    size = 0.1
                }
            }

            layer {
                draw {
                    drawer.image(rt.colorBuffer(0))
                }
            }
        }

        extend(Screenshots())
        extend(ScreenRecorder()){
            maximumDuration = 2*PI
        }
        extend {
            drawer.isolatedWithTarget(rt) {
                drawStyle.blendMode = BlendMode.BLEND
                clear(ColorRGBa.TRANSPARENT)
                fill = ColorRGBa.PINK
                circle(width/2.0, height/2.0, 200.0)
                drawStyle.blendMode = BlendMode.REMOVE
                fill = ColorRGBa(0.1, 0.7, 1.0, sin(seconds+PI/2)*0.5+0.5)
                stroke = null
                circle(width/3.0, height/3.0, 250.0)
            }
            c.draw(drawer)
        }
    }
}