package useful

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Matrix44

// from https://guide.openrndr.org/#/11_Advanced_Topics/C03_Writing_extensions

class FrameBorder : Extension {
    override var enabled: Boolean = true

    override fun afterDraw(drawer: Drawer, program: Program) {

    }
}