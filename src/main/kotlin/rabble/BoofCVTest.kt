import boofcv.alg.filter.binary.BinaryImageOps
import boofcv.alg.filter.binary.GThresholdImageOps
import boofcv.alg.filter.binary.ThresholdImageOps
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors
import boofcv.struct.ConnectRule
import boofcv.struct.image.GrayS16
import boofcv.struct.image.GrayU8
import org.openrndr.application
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.boofcv.binding.toGrayU8
import org.openrndr.boofcv.binding.toShapeContours
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.math.CatmullRomChain2
import org.openrndr.math.Vector2
import org.openrndr.shape.*

fun main() {
    application {
        configure {
            width = 640
            height = 480
        }

        oliveProgram {
            val gui = GUI()
            val settings = object {
                @DoubleParameter("low", 0.0, 1.0)
                var low: Double = 0.1

                @DoubleParameter("high", 0.0, 1.0)
                var high: Double = 0.3
            }
            gui.add(settings, "Edge detection settings")

            val video = loadVideoDevice("HP HD Camera")
            video.play()

            val videoFrame = colorBuffer(video.width, video.height)
//            video.newFrame.listen {
//                it.frame.copyTo(videoFrame, targetRectangle = IntRectangle(0, videoFrame.height - (videoFrame.height - height) / 2, it.frame.width, -it.frame.height))
//            }

            extend(gui)
            extend {
//                val cs = imageToContours(videoFrame)
                val cs = imageEdgeFinder(videoFrame, settings.low, settings.high)
                if (frameCount % 180 == 0){
                    println(cs.size)
                }

                drawer.apply {
                    video.draw(this)
                    fill = ColorRGBa.PINK.opacify(0.25)
                    stroke = ColorRGBa.PINK.opacify(0.8)
//                    contours(polygonal)
//                    contours(smooth)
                    contours(cs)
//                    drawer.image(videoFrame)
//                    image(rt.colorBuffer(0))

                }
            }
        }
    }
}

fun imageToContours(input: ColorBuffer): List<ShapeContour> {
    val bitmap = input.toGrayF32()
    // BoofCV: calculate a good threshold for the loaded image
    val threshold = GThresholdImageOps.computeOtsu(bitmap, 0.0, 255.0)

    // BoofCV: use the threshold to convert the image to black and white
    val binary = GrayU8(bitmap.width, bitmap.height)
    ThresholdImageOps.threshold(bitmap, binary, threshold.toFloat(), false)

    // BoofCV: Contract and expand the white areas to remove noise
    var filtered = BinaryImageOps.erode8(binary, 1, null)
    filtered = BinaryImageOps.dilate8(filtered, 1, null)

    // BoofCV: Calculate contours as vector data
    val contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null)

    // orx-boofcv: convert vector data to OPENRNDR ShapeContours
    return contours.toShapeContours(true, internal = true, external = true)
}

fun imageEdgeFinder(input: ColorBuffer, threshLow: Double, threshHigh: Double): List<ShapeContour> {
    val bitmap = input.toGrayU8()
//    // BoofCV: calculate a good threshold for the loaded image
//    val threshold = GThresholdImageOps.computeOtsu(bitmap, 0.0, 255.0)
//
//    // BoofCV: use the threshold to convert the image to black and white
//    val binary = GrayU8(bitmap.width, bitmap.height)
//    ThresholdImageOps.threshold(bitmap, binary, threshold.toFloat(), false)
//
//    // BoofCV: Contract and expand the white areas to remove noise
//    var filtered = BinaryImageOps.erode8(binary, 1, null)
//    filtered = BinaryImageOps.dilate8(filtered, 1, null)
    val edgeImage = bitmap.createSameShape()

    val canny = FactoryEdgeDetectors.canny(2,true, true, GrayU8::class.java, GrayS16::class.java)

    canny.process(bitmap, threshLow.toFloat(), threshHigh.toFloat(), edgeImage)

    val contours = BinaryImageOps.contourExternal(edgeImage, ConnectRule.EIGHT)
    return contours.toShapeContours(true, internal = false, external = true)
}