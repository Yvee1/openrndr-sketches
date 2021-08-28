package indra

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.extra.videoprofiles.PNGProfile
import org.openrndr.extras.color.presets.DARK_BLUE
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import useful.FPSDisplay
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

//const val ratioMax = 2 * sqrt(3.0) + 3
private const val ratioMax = 6.46410161514
private const val RECORD = true

fun main() = applicationSynchronous {
    configure {
        windowResizable = true
        width = 200
        height = 200
    }

    program {
        val s = object : Animatable() {
            @DoubleParameter("Ratio", 1.0, ratioMax, order=0)
            var ratio : Double = 2.0

            @DoubleParameter("Base radius", 5.0, 100.0, order=1)
            var baseRadius : Double = 60.0

            @ColorParameter("Small circle color", order=2)
            var smallColor : ColorRGBa = ColorRGBa.GREEN

            @ColorParameter("Big circle color", order=3)
            var bigColor : ColorRGBa = ColorRGBa.RED

            @ColorParameter("Background color", order=4)
            var bgColor : ColorRGBa = ColorRGBa.DARK_BLUE

            @ColorParameter("Stroke color", order=5)
            var strokeColor : ColorRGBa = ColorRGBa.BLACK

            @DoubleParameter("Stroke weight", 0.0, 10.0, order=6)
            var strokeWeight = 1.0

            @DoubleParameter("Extra radius", 0.0, 4.0, order=7)
            var extra = 1.5

            @DoubleParameter("Animation duration", 0.1, 10.0, order=8)
            var duration = 4.0
        }

        val gui = GUI()
        gui.add(s, "Settings")

        fun draw() {
            val v = Vector2(s.baseRadius + s.ratio * s.baseRadius, 0.0)

            fun small() = drawer.isolated {
                fill = s.smallColor
                circle(Vector2.ZERO, s.baseRadius + s.extra)
            }

            fun big() = drawer.isolated {
                fill = s.bigColor
                circle(Vector2.ZERO, s.ratio * s.baseRadius + s.extra)
            }

            drawer.isolated {
                strokeWeight = s.strokeWeight
                stroke = s.strokeColor

                clear(s.bgColor)
                translate(width/2.0, height/2.0)

                val w = ceil(width / (v.length*3)).toInt()
//                val h = (w*0.75).toInt()
                val h = ceil(height/ (v.length*3)).toInt()
                print("w: $w; h: $h\r")

//                val w = 3
//                val h = 2

                for (y in -h until h+1) isolated {
                    translate(v.rotate(60.0) * 3.0 * y.toDouble())
                    for (x in -w until w + 1) isolated {
                        translate(v * 3.0 * x.toDouble())
                        big()

                        for (i in 0 until 6) isolated {
                            rotate(i*60.0)
                            translate(v)
                            small()
                        }

                        for (i in 0 until 2) isolated {
                            rotate(i*60.0)
                            translate(v)
                            rotate(60.0)
                            translate(v)
                            big()
                        }
                    }
                }
            }
        }

        gui.doubleBind = true

        fun animate() {
            if (!s.hasAnimations()) {
                s.ratio = 1.0
                s.apply {
                    val halfDuration = (s.duration/2.0 * 1000).toLong()
                    ::ratio.animate(ratioMax, halfDuration, Easing.CubicInOut)
                    ::ratio.complete()
                    ::ratio.animate(1.0, halfDuration, Easing.CubicInOut)
                }
            }
            s.updateAnimation()
        }

        if (!RECORD) {
            extend(gui)
            extend(FPSDisplay())
        } else {
            extend(ScreenRecorder()) {
                profile = PNGProfile()
//                profile = GIFProfile()
                frameRate = 50
//                frameRate = 60
                maximumDuration = s.duration
                outputFile = "small-animation/frame-%03d.png"
            }
//            extend(TemporalBlur()) {
//                samples = 20
//                duration = 1.0
////            colorMatrix = {
////                // `it` is 0.0 at start of frame, 1.0 at end of frame
////                tint(ColorRGBa.WHITE.mix(ColorRGBa.BLUE, it))
////            }
//                gain = 1.2
//                jitter = 0.0
//            }
        }

        val font = loadFont("data/fonts/default.otf", 18.0)
        extend {
            drawer.fontMap = font
//            drawer.scale(0.2)
            animate()
            draw()
        }
    }
}