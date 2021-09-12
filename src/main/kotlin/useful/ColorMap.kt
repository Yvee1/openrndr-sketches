package useful

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import java.io.File

interface ColorMap {
    /**
     * @param t Parameter between 0 and 1
     */
    fun getColor(t: Double): ColorRGBa
}

fun getFileColors(f: File) = f.readLines().map {
    val rgb = it.split(' ').map { it.toDouble() }
    ColorRGBa(rgb[0], rgb[1], rgb[2])
}

abstract class ColorMapFile() : ColorMap {
    abstract val colors: List<ColorRGBa>
    override fun getColor(t: Double): ColorRGBa = colors[((t-0.00001) * colors.size).toInt()]
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