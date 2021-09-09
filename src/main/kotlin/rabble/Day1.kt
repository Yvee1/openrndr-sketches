package genuary

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSVa
import org.openrndr.color.Linearity
import org.openrndr.draw.BlendMode
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.dither.CMYKHalftone
import org.openrndr.extra.fx.dither.Crosshatch
import org.openrndr.extra.fx.shadow.DropShadow
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.*
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.ShapeContour.Companion.fromPoints
import org.openrndr.shape.Triangle
import kotlin.math.*
import org.openrndr.extra.palette.PaletteStudio
import org.openrndr.extra.videoprofiles.GIFProfile

fun main() = application {
    configure {
        width = 1000
        height = 1000
        position = IntVector2(800, 10)
    }

    oliveProgram {
        val paletteStudio = PaletteStudio(
            loadDefault = true, // Loads the first collection of palettes. [default -> true]
            sortBy = PaletteStudio.SortBy.DARKEST, // Sorts the colors by luminance. [default -> PaletteStudio.SortBy.NO_SORTING]
            collection = PaletteStudio.Collections.ONE, // Chooses which collection to load [default -> Collections.ONE]
            colorCountConstraint = 5 // Constraints the number of colors in the palette [default -> 0]
        )

        val RECORD = false
        val GIF = true

        val gui = GUI()

        val colors = arrayOf(ColorRGBa(r=0.9764705882352941, g=0.9686274509803922, b=0.9686274509803922, a=1.0, linearity= Linearity.SRGB), ColorRGBa(r=0.8588235294117647, g=0.8862745098039215, b=0.9372549019607843, a=1.0, linearity= Linearity.SRGB), ColorRGBa(r=0.24705882352941178, g=0.4470588235294118, b=0.6862745098039216, a=1.0, linearity= Linearity.SRGB), ColorRGBa(r=0.06666666666666667, g=0.17647058823529413, b=0.3058823529411765, a=1.0, linearity= Linearity.SRGB))

        extend(Screenshots())
        if (RECORD) {
            extend(ScreenRecorder()) {
                if (GIF) {
                    profile = GIFProfile()
                    frameRate = 50
                } else {
                    frameRate = 60
                }
            }
//
//            extend(TemporalBlur()) {
//                duration = 0.95
//                samples = 60
//                if (GIF){
//                    fps = 50.0
//                } else {
//                    fps = 60.0
//                }
//                jitter = 1.0
//            }
        } else {
            extend(gui)
        }

        var rounds = 0

        extend(paletteStudio)
        keyboard.keyDown.listen {
            if (it.name == "p") {
                print(paletteStudio.colors)
                print("\n")
            }
        }

        extend {
            if (seconds > 2*PI && RECORD){
                application.exit()
            }

            fun regularTriangleAt(x: Double, y: Double, r: Double): Triangle {
                val points = Array(3) { i ->
                    Vector2(x + r * cos(i * 2*PI/3 - PI/2), y + r * sin(i * 2*PI/3 - PI/2))
                }
                return Triangle(points[0], points[1], points[2])
            }

            drawer.apply {
//                clear(ColorRGBa.BLACK)
                clear(paletteStudio.colors[0])
//                translate(width/2.0, height/2.0)
                fill = ColorRGBa.WHITE
                stroke = ColorRGBa.WHITE
//                stroke = ColorRGBa.RED
                strokeWeight = 2.0
//                circle(Vector2.ZERO, 100.0)

                // We have to map iterators to variables
                // IDEA 1: x, y, radius
                val n = 10
                for (i in 0 until n) {
                    for (j in 0 until n) {
                        for (k in 1 until 4) {
                            rotate(i+j+k+0.0)
//
//                            drawer.drawStyle.blendMode = BlendMode.ADD
                            //// 1
                            fill = ColorXSVa(120.0 - k / 5.0 * 120.0, 1.0, 1.0).toRGBa()
                            stroke = null
                            fill = paletteStudio.colors[k]
                            circle((i+1) * width/(n+1.0)+30.0*cos(seconds+i+j) - 100.0, (j+1) * height/(n+1.0)+30.0*sin(seconds+i+j), (sin(seconds+i+j)+1)*k*10.0)
                            //// 2
//                            stroke = ColorXSVa(120.0 - k / 5.0 * 120.0, 1.0, 1.0).toRGBa()
//                            contour(Circle((i+1) * width/(n+1.0), (j+1) * height/(n+1.0), (k+1)*5.0*n/(n+1) - 3.0).contour.sub(0.0, (i+j)/(2.0*(n-1))))
                            //// 3

//                            stroke = ColorXSVa(120.0 - k / 6.0 * 120.0, 1.0, 1.0).toRGBa()
//                            contour(regularTriangleAt((i+1) * width/(n+1.0), (j+1) * height/(n+1.0), (k+1)*10.0*n/(n+1) - 3.0).contour.sub(0.0, (i+j)/(2.0*(n-1))))
                        }
                    }
                }

                // IDEA 2: x, y, t: t is between 0 and 1 for sub
//                for (i in 0 until n) {
//                    for (j in 0 until n) {
//                        for (t in 0 until n) {
//                            contour(Triangle.regularAt((i+1) * width/(n+1.0), (j+1) * height/(n+1.0), 10.0).contour.sub(0.0, (i+j)/(2.0*(n-1))))
//                        }
//                    }
//                }
            }
        }
    }
}
