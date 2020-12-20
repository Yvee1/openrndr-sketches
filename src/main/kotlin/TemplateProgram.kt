import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.draw.tint
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriterProfile
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {
        val image = loadImage("data/images/pm5544.png")
        val font = loadFont("data/fonts/default.otf", 64.0)

        extend(ScreenRecorder()) {
            profile = GIFProfile()
            frameRate = 30
        }

        extend(TemporalBlur()) {
            duration = 2.0
            samples = 30
            fps = 30.0
            jitter = 1.0
        }

        extend {
            drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.shade(0.2))
            drawer.image(image)

            drawer.fill = ColorRGBa.PINK
            drawer.circle(cos(seconds) * width / 2.0 + width / 2.0, sin(0.5 * seconds) * height / 2.0 + height / 2.0, 140.0)

            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.text("OPENRNDR", width / 2.0, height / 2.0)

            if (frameCount > 150){
                application.exit()
            }
        }
    }
}

class GIFProfile : VideoWriterProfile() {
    override val fileExtension = "gif"

    override fun arguments(): Array<String> {
        return arrayOf("-vf", "split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse=dither=none:diff_mode=rectangle,vflip")
    }
}