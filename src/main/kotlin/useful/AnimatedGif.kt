package useful

import kotlinx.coroutines.runBlocking
import org.openrndr.*
import org.openrndr.animatable.Animatable
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.XYParameter
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2

private val configuration = Configuration().apply {
    width = 1000
    height = 1000
    windowResizable = true
}

class AnimatedGifProgram(RECORD: Boolean, GIF: Boolean) : Program() {
    var duration = 5.0
    val gui = GUI()

    init {
        gui.compartmentsCollapsedByDefault = true
        if(RECORD){
            extend(ScreenRecorder()){
                if (GIF) {
                    profile = GIFProfile()
                    frameRate = 50
                } else {
                    frameRate = 60
                }
                maximumDuration = duration
            }
        } else {
            extend(gui)
            extend(Screenshots())
        }
    }
}

private val programInit: Program.() -> Unit = {

}

fun animatedGif(RECORD: Boolean, GIF: Boolean, build: CustomBuilder.() -> Unit) {
    val builder = CustomBuilder(configuration, AnimatedGifProgram(RECORD, GIF)).apply { build() }
    runBlocking { application(builder.program, builder.configuration) }
}

fun main() = animatedGif(RECORD=false, GIF=true) {
    program {
        extend {
            drawer.circle(mouse.position, 100.0)
        }
    }
}
