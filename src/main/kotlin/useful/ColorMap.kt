package useful

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector2
import org.openrndr.shape.CompositionDrawer
import java.io.File

interface ColorMap {
    /**
     * @param t Parameter between 0 and 1
     */
    fun getColor(t: Double): ColorRGBa
    fun Drawer.drawColorBar(pos: Vector2, w: Double, h: Double)
    fun CompositionDrawer.drawColorBar(pos: Vector2, w: Double, h: Double)
}

fun getFileColors(f: File) = f.readLines().map {
    val rgb = it.split(' ').map { it.toDouble() }
    ColorRGBa(rgb[0], rgb[1], rgb[2])
}

abstract class ColorMapFile() : ColorMap {
    abstract val colors: List<ColorRGBa>
    override fun getColor(t: Double): ColorRGBa = colors[((t-0.00001) * colors.size).toInt()]
    override fun Drawer.drawColorBar(pos: Vector2, w: Double, h: Double) {
        isolated {
            translate(pos)
            stroke = null
            val cellSize = h / 256
            for (i in 0 until 256){
                fill = getColor(i / 256.0)
                rectangle(0.0, cellSize * i, w, cellSize)
            }
        }
    }
    override fun CompositionDrawer.drawColorBar(pos: Vector2, w: Double, h: Double) {
        isolated {
            translate(pos)
            stroke = null
            val cellSize = h / 256
            for (i in 0 until 256){
                fill = getColor(i / 256.0)
                rectangle(0.0, cellSize * i, w, cellSize*1.5)
            }
        }
    }
}

// Files from https://github.com/Jsalam/JViridis/
class Viridis : ColorMapFile() {
    override val colors = getFileColors(File("data/colormaps/viridis.cmap"))
}

class Inferno : ColorMapFile() {
    override val colors = getFileColors(File("data/colormaps/inferno.cmap"))
}

class Plasma : ColorMapFile() {
    override val colors = getFileColors(File("data/colormaps/plasma.cmap"))
}

class Magma : ColorMapFile() {
    override val colors = getFileColors(File("data/colormaps/magma.cmap"))
}


fun main() = application {
    program {
        val colormaps = listOf(Viridis(), Inferno(), Plasma(), Magma())
        extend {
            drawer.apply {
                clear(ColorRGBa.WHITE)
                val s = 1.0

                colormaps.forEachIndexed { j, cm ->
                    for (i in 0 until 256) {
                        stroke = null
                        fill = cm.getColor(i / 256.0)
                        rectangle(i * s, j*20*s, s, 10 * s)
                    }
                }
            }
        }
    }
}