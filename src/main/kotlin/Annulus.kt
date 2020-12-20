import org.openrndr.Fullscreen
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.Clock
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSVa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.shadow.DropShadow
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.olive.Olive
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.*
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.extra.temporalblur.*
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriterProfile
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.Vector2.Companion.fromPolar
import org.openrndr.shape.*
import kotlin.math.*

/**
 *  See https://math.stackexchange.com/questions/3954883/finding-angles-of-non-overlapping-adjacent-triangles-whose-vertices-lie-on-the-e
 *  and https://en.wikipedia.org/wiki/Annulus_(mathematics)#/media/File:Mamikon_annulus_area_visualisation.svg
 */

const val RECORD = true
const val GIF = true

fun main() = application {
    configure {
        width = 1000
        height = 1000
    }

    program {
        val tex = loadImage("data/images/cheeta.jpg")

        val gui = GUI()

        val s = object : Animatable() {
            @IntParameter("Segments", 3, 20)
            var segments: Int = 15

            @DoubleParameter("Inner radius", 1.0, 2000.0)
            var innerRadius: Double = 190.0

            @DoubleParameter("Outer radius", 50.0, 2000.0)
            var outerRadius: Double = 300.0

            @DoubleParameter("Rotation", -PI, PI)
            var rotation: Double = 0.0

            @ColorParameter("Background")
//            var bg: ColorRGBa = ColorRGBa(0.33, 0.33, 0.43)
            var bg: ColorRGBa = ColorRGBa(0.95, 0.95, 0.85)

            @DoubleParameter("Gain", 0.0, 1.0)
            var gain: Double = 0.4
        }
        gui.add(s, "Settings")

        val center = Vector2(width/2.0, height/2.0)

        val shadow = DropShadow()
        val bloom = GaussianBloom()

        val comp = compose {
            layer {
                draw {
//                    drawer.clear(ColorRGBa(0.2, 0.2, 0.2))
                    drawer.clear(s.bg)
                }
            }
            layer {
                draw {
                    drawer.translate(center.x, height.toDouble() - center.y)
                    drawer.rotate(toDegrees(s.rotation))
                    drawer.scale(1.0, -1.0)

                    val r = s.innerRadius
                    val R = s.outerRadius
                    val n = s.segments

                    val alpha = acos((r / R) * cos(PI / n)) - PI / n

                    for (j in 0 until s.segments) {
                        val i = n - j - 1

                        // Current angle
                        val ca: Double = i / n.toDouble() * PI * 2 + PI
                        // Next angle
                        val na: Double = (i + 1) / n.toDouble() * PI * 2 + PI

                        val p1 = Vector2(cos(ca), sin(ca)) * R
                        val p2 = Vector2(cos(na), sin(na)) * R
                        val p3 = p2.rotate(toDegrees(1.19*alpha)).normalized * r

                        val c = contour {
                            moveTo(p1)
                            lineTo(p2)
                            curveTo((p1+p2)/2.0, p3)
                            curveTo((p1+p2)/2.0, p1)
                            close()
                        }

                        drawer.stroke = ColorRGBa.BLACK
                        drawer.shadeStyle = linearGradient(ColorRGBa(0.15, 0.15, 0.15), ColorRGBa.GRAY, rotation = toDegrees(ca)+90)

//                        drawer.shadeStyle = shadeStyle {
//                            fragmentTransform = "x_fill = texture(p_tex, va_texCoord0.xy);"
//                            parameter("tex", tex)
//                        }
//                        drawer.stroke = null
//                        drawer.fill = ColorXSVa(i.toDouble() / s.segments * 360.0, 0.85, 0.95, 1.0).toRGBa()
//                        drawer.fill = ColorRGBa.GRAY
                        drawer.contour(c)
                    }
                }
                post(bloom).addTo(gui)
                post(shadow).addTo(gui)
            }

//            layer {
//                draw {
//
//                }
//            }
        }

        var rounds = 0
        var spacing = 50
        var current = 0

        extend(Screenshots()) {
            scale = 1.0
        }
        if (RECORD) {
            extend(ScreenRecorder()) {
                if (GIF) {
                    profile = GIFProfile()
                    frameRate = 50
                } else {
                    frameRate = 60
                }
            }
        } else {
            extend(gui)
        }

        extend(TemporalBlur()) {
            duration = 0.5
            samples = 30
            fps = 50.0
            jitter = 1.0
        }

        extend {
            if (!s.hasAnimations()) {
                if (rounds > 0 && RECORD){
                    application.exit()
                } else {
                    s.apply {
                        animate("innerRadius", 40.0, 1000, Easing.QuartInOut)
                        animate("gain", 0.6, 1000, Easing.QuartInOut)
                        complete()
//                        delay(50)
                        animate("innerRadius", 10.0, 300, Easing.QuadInOut)
                        animate("gain", 1.0, 300, Easing.QuartInOut)
                        complete()
                        animate("innerRadius", 20.0, 500, Easing.QuadInOut)
                        animate("gain", 0.8, 500, Easing.QuartInOut)
                        complete()
                        animate("innerRadius", 15.0, 1100, Easing.QuadInOut)
                        animate("gain", 0.9, 1100, Easing.QuartInOut)
                        complete()
                        delay(100)
                        animate("innerRadius", 190.0, 800, Easing.QuartInOut)
                        animate("gain", 0.4, 1100, Easing.QuartInOut)
                        complete()
//                        animate("innerRadius", 110.0, 500, Easing.CubicInOut)
//                        complete()
                    }
                }
                rounds++
            }

            s.updateAnimation()

//            shadow.gain = s.gain
//            shadow.xShift = 6.0
//            shadow.yShift = -3.0
//            if (shadow.gain > 0.9){
//                shadow.window = 1
//            } else if (shadow.gain > 0.7){
//                shadow.window = 2
//            } else {
//                shadow.window = 4
//            }
            bloom.sigma = 0.0

            comp.draw(drawer)
        }
    }
}

fun toDegrees(radians: Double): Double{
    return radians / PI * 180;
}