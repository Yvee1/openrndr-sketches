import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import org.openrndr.extra.olive.oliveProgram
import java.io.File
import kotlin.math.*

fun main() = applicationSynchronous {
    configure {
        width = 1000
        height = 1000
    }

    oliveProgram {
        val image = loadImage("data/images/sunflower.png")
        val shadow = image.shadow
        shadow.download()

        val skip = 0
        val off = 150
        val fitShift = 240

        val newWidth = 1000
//        val w = sqrt(image.effectiveHeight.toDouble().pow(2) + image.effectiveWidth.toDouble().pow(2)).roundToInt()
        val w = image.effectiveWidth + 0
        val newHeight = w / 2 - skip - fitShift + 100

        val rt = renderTarget(newWidth, newHeight) {
            colorBuffer()
        }

        drawer.isolatedWithTarget(rt){
            clear(ColorRGBa.BLACK)
            ortho(rt)
            for (i in 0 until newWidth) {
                for (r in off until newHeight + off) {
                    val angle = i / newWidth.toDouble() * 2 * PI
//                    if (r < newHeight + fitShift) {
                        var x = (r * cos(angle) + w / 2.0).roundToInt()
                        var y = (r * sin(angle) + w / 2.0).roundToInt()
                        var color = if (x < image.effectiveWidth && x >= 0 && y < image.effectiveHeight && y >= 0) shadow[x, y] else ColorRGBa.BLACK
//                        if (x == (image.effectiveWidth / 2.0).roundToInt()) {
//                            println(y)
//                        }

                        // Are we not at a transparent part of the image? Then we just draw the transformed pixel
                        if (color.a > 0.5 && color != ColorRGBa.BLACK) {
                            stroke = null
                            drawer.fill = color
                            rectangle(i.toDouble(), (r - off).toDouble(), 1.0, 1.0)
                            continue
                        }
//                    }

//                    if (r - newHeight >= 0) {
                        x = ((r - newHeight + skip) * cos(angle) + w / 2.0).roundToInt()
                        y = ((r - newHeight + skip) * sin(angle) + w / 2.0).roundToInt()
                        color = if (x < image.effectiveWidth && y < image.effectiveHeight) shadow[x, y] else ColorRGBa.BLACK
//                    if (x == (image.effectiveWidth / 2.0).roundToInt()) {
//                        println(y)
//                    }
                        stroke = null
                        drawer.fill = color
                        rectangle(i.toDouble(), (r - off).toDouble(), 1.0, 1.0)
//                    }
                }
            }
        }

        extend {
            drawer.isolated {
                clear(ColorRGBa.BLACK)
                stroke = ColorRGBa.GREEN
                fill = null
                rectangle(0.0, 0.0, newWidth*1.0, newHeight*1.0)
                image(rt.colorBuffer(0))
                for (i in 0 until 3) {
                    translate(0.0, newHeight*1.0)
                    stroke = ColorRGBa.GREEN
                    fill = null
                    rectangle(0.0, 0.0, newWidth*1.0, newHeight*1.0)
                    image(rt.colorBuffer(0))
                }
            }
        }

        rt.colorBuffer(0).saveToFile(File("sunflowerTile.png"))
    }
}