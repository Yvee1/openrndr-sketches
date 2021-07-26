import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.orml.styletransfer.StyleEncoder
import org.openrndr.orml.styletransfer.StyleTransformer
import org.openrndr.shape.IntRectangle

fun main() = application {
    program {
        val encoder = StyleEncoder.load()
        val transformer = StyleTransformer.loadSeparable()

        val video = loadVideoDevice("HP HD Camera", width = 640, height = 480)
        video.play()

        val videoFrame = colorBuffer(video.width, video.height)
        video.newFrame.listen {
            it.frame.copyTo(videoFrame, targetRectangle = IntRectangle(0, videoFrame.height - (videoFrame.height - 480) / 2, it.frame.width, -it.frame.height))
        }

        val img = loadImage("data/images/sunflower.png")

        var style = FloatArray(0)
        extend {
            video.draw(drawer)
            style = encoder.encodeStyle(img)
            val transformed = transformer.transformStyle(videoFrame, style)
            drawer.image(transformed)
        }
    }
}