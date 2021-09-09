package current

import org.openrndr.application
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import kotlin.concurrent.thread

fun main() = application {
    program {
        var position = Vector2.ZERO
        val bgColor = ColorRGBa.fromHex("#ece6e2")

        extend {
            if (frameCount % 120 == 0){
                println("Application 1 running on thread ${Thread.currentThread().name}")
            }

            drawer.clear(bgColor)
            drawer.fill = ColorRGBa.fromHex("#85c3ca")
            drawer.circle(Vector2(width*1.0, height*1.0) - position, 100.0)
        }

        thread {
            application {
                program {
                    extend {
                        if (frameCount % 120 == 0) {
                            println("Application 2 running on thread ${Thread.currentThread().name}")
                        }
                        position = mouse.position
                        drawer.clear(bgColor)
                        drawer.fill = ColorRGBa.fromHex("#e07a5f")
                        drawer.circle(position, 50.0)
                    }
                }
            }
        }
    }
}