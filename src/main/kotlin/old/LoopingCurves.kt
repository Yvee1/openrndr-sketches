import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        val duration = 5.0

        val settings = object {
            @DoubleParameter("Delay 1", 0.0, 10.0)
            var delay1 = 2.0

            @DoubleParameter("Delay 2", 0.0, 10.0)
            var delay2 = 0.5

            @IntParameter("Resolution", 1, 100000)
            var res = 10000
        }

        val gui = GUI()
        gui.add(settings, "Settings")

        extend(gui)
        extend {
            val playhead = seconds/duration

            fun pos1(t: Double) = Vector2(width*0.25, height*0.5) + Vector2(cos(t*2*PI*2.0), sin(t*2*PI*2.0))*150.0
            fun pos2(t: Double) = Vector2(width*0.75, height*0.5) + Vector2(cos(t*2*PI), sin(t*2*PI))*150.0

            drawer.apply {
                circle(pos1(playhead), 10.0)
                circle(pos2(playhead), 10.0)
            }

            val pts = mutableListOf<Vector2>()

            for (i in 0 until settings.res){
                val t = i/settings.res.toDouble()
                val pos = pos1(playhead - settings.delay1*t)*(1-t) + pos2(playhead - settings.delay2*(1-t))*t
                pts.add(pos)
            }
            drawer.points(pts)
        }
    }
}