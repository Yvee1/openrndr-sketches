package useful

import org.openrndr.application
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import java.io.File
import kotlin.math.abs

fun main() = application {
    program {
        val w = 200
        val h = 200
        val n = 200
        val frames = List(n){  loadImage("small-animation/frame-${(it + 1).toString().padStart(3, '0')}.png").shadow }
        frames.forEach { it.download() }

        for (i in 0 until 200){
            print("Frame: ${i}\r")

            val rt = renderTarget(w, h) { colorBuffer() }
            drawer.isolatedWithTarget(rt) {
                for (x in 0 until w) {
                    for (y in 0 until h) {
                        val offset = (abs(x - w / 2) + abs(y - h / 2)) / 5
                        val frameNumber = (i + offset) % n
                        val color = frames[frameNumber][x, y]
                        stroke = null
                        fill = color
                        rectangle(x.toDouble(), y.toDouble(), 1.0, 1.0)
                    }
                }
            }
            rt.colorBuffer(0).saveToFile(File("output/frame-${(i + 1).toString().padStart(3, '0')}.png"))
        }
    }
}