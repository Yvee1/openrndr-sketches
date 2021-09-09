package useful

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.math.Matrix44

// from https://guide.openrndr.org/#/11_Advanced_Topics/C03_Writing_extensions

class FPSDisplay : Extension {
    override var enabled: Boolean = true

    var frames = 1
    var smoothing = 0.9
    var avg = 1.0
    var lastFrameDraw = 0.0
    var textColor = ColorRGBa.BLACK

    override fun setup(program: Program) {

    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        program.drawer.fontMap = loadFont("data/fonts/default.otf", 18.0)
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        frames++


        val delta = program.seconds - lastFrameDraw
        avg = avg * smoothing + ((1.0 / delta * (1.0 - smoothing)))

        drawer.isolated {
            // -- set view and projections
            view = Matrix44.IDENTITY
            ortho()
            shadeStyle = null

            fill = textColor
            text("fps: ${avg.toInt()}", 0.0, 15.0)
        }

        lastFrameDraw = program.seconds
    }
}