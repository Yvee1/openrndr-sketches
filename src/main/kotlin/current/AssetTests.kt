package current

import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.gitarchiver.GitArchiver
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.DoubleParameter

fun main() = application {
    program {
        val s = object {
            @DoubleParameter("Radius", 10.0, 300.0)
            var radius = 50.0
        }

        val gui = GUI()
        gui.add(s)

        extend(GitArchiver())
        extend(Screenshots())
        extend(gui)
        extend {
            drawer.circle(width/2.0, height/2.0, s.radius)
        }
    }
}