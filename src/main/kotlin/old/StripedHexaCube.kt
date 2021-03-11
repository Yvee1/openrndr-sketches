import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSVa
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
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.ShapeContour.Companion.fromPoints
import kotlin.math.*

fun main() = application {
    configure {
        width = 800
        height = 800
        position = IntVector2(1400, 200)
    }

    program {
        val RECORD = false
        val GIF = true

        val gui = GUI()

        val s = object : Animatable() {
            @IntParameter("Stripes", 0, 20)
            var stripes: Int = 3

            @DoubleParameter("Radius", 1.0, 500.0)
            var r: Double = width/5.0

            @IntParameter("x-repeat", 0, 20)
            var xRepeat: Int = 2

            @IntParameter("y-repeat", 0, 20)
            var yRepeat: Int = 2

            @ColorParameter("Background")
            var bg: ColorRGBa = ColorRGBa.BLACK

            @DoubleParameter("Offset", -0.1, 0.2)
            var offset: Double = -0.075

            @DoubleParameter("Rotated cube", 0.0, 120.0)
            var rotated: Double = 0.0

            @DoubleParameter("Rotated tiling", -180.0, 180.0)
            var rotatedTiling: Double = 0.0
        }
        gui.add(s, "Settings")

        val color = arrayOf(
            ColorRGBa.fromHex("#FAA613"),
            ColorRGBa.fromHex("#688E26"),
            ColorRGBa.fromHex("#F44708"),
        )

        if (RECORD) {
            extend(ScreenRecorder()) {
                if (GIF) {
                    profile = GIFProfile()
                    frameRate = 50
                } else {
                    frameRate = 60
                }
            }

            extend(TemporalBlur()) {
                duration = 0.95
                samples = 60
                if (GIF){
                    fps = 50.0
                } else {
                    fps = 60.0
                }
                jitter = 1.0
            }
        } else {
            extend(gui)
        }

        val comp = compose {
            val n = 6
            val r = s.r
            val stripes = s.stripes

            val points = List(n) { i ->
                val theta = 2*PI/n * i + PI/n
                Vector2(r * cos(theta), r * sin(theta))
            }

            fun outer(i: Int): LineSegment {
                return LineSegment(points[(i+1) % n], points[i % n])
            }

            fun inner(i: Int): LineSegment {
                // precondition: i is in [0, n/2)
                return LineSegment(Vector2.ZERO, points[2*i+1])
            }

            fun ShapeContour.movingPositions(n:Int, t: Double, reversed: Boolean = false, offset: Double = 0.0): List<Pair<Double, Vector2>> {
                if (!reversed) {
                    return List(n + 2) { i ->
                        val ut = (i - 2.0) / n + t / stripes + (if (i % 2 == 0) -offset else offset)
                        Pair(ut, this.position(ut)) }
                } else {
                    return List(n + 2) { i ->
                        val ut = 1.0 - ((i - 2.0) / n + t / stripes) + (if (i % 2 == 1) -offset else offset)
                        Pair(ut, this.position(ut)) }
                }
            }

            fun LineSegment.points(reversed: Boolean = false): List<Pair<Double, Vector2>>{
                return this.contour.movingPositions(stripes*2, (seconds+1.5) / 3.5 % 1, reversed=reversed, offset = if (s.offset > 0.09) 0.3 else s.offset)
            }

            fun drawSide(drawer: Drawer, innerSide: List<Pair<Double, Vector2>>, outerSide: List<Pair<Double, Vector2>>){
                for (i in 0 until stripes + 1) {
                    if (outerSide[2 * i + 1].first > 0 && outerSide[2*i].first < 1) {
                        val pts = listOf(
                            innerSide[2 * i].second,
                            outerSide[2 * i].second,
                            outerSide[2 * i + 1].second,
                            innerSide[2 * i + 1].second,
                            innerSide[2 * i].second
                        )
                        drawer.contour(fromPoints(pts, true))
                    }
                }
            }

            layer {
                draw {
                    drawer.clear(s.bg)
                }
            }

            layer {
                draw {
                    drawer.translate(width / 2.0, height / 2.0)
                    drawer.rotate(s.rotatedTiling)

                    for (k in -s.yRepeat until s.yRepeat + 1) {
                        for (j in -s.xRepeat until s.xRepeat + 1) {
                            val kind = Math.floorMod(j + 2 * (k % 2), 3)

                            drawer.isolated {
                                translate((j + (k % 2) * 0.5) * cos(PI / 6) * r * 2, k * 1.5 * r)

                                if (kind == 0) {
                                    rotate(120.0 - s.rotated)
                                }

                                if (kind == 1) {
                                    rotate(120.0 - s.rotated)
                                }

                                if (kind == 2) {
                                    rotate(120.0 - s.rotated)
                                }

                                strokeWeight = 0.1

                                val i0ps = inner(0).points()
                                val i0ps_ = inner(0).points(reversed = true)
                                val i1ps = inner(1).points()
                                val i1ps_ = inner(1).points(reversed = true)
                                val i2ps = inner(2).points()
                                val i2ps_ = inner(2).points(reversed = true)
                                val o1ps = outer(1).points()
                                val o2ps = outer(2).points()
                                val o3ps = outer(3).points()
                                val o4ps = outer(4).points()
                                val o5ps = outer(5).points()
                                val o0ps = outer(0).points()

                                when (kind) {
                                    0 -> {
                                        fill = color[0]
                                        stroke = color[0]
                                        drawSide(this, i0ps, o2ps)

                                        fill = color[1]
                                        stroke = color[1]
                                        drawSide(this, i1ps, o4ps)

                                        fill = color[1]
                                        stroke = color[1]
                                        stroke = color[1]
                                        drawSide(this, i0ps_, o5ps)
                                    }

                                    1 -> {
                                        fill = color[0]
                                        stroke = color[0]
                                        drawSide(this, i0ps, o2ps)

                                        fill = color[0]
                                        stroke = color[0]
                                        drawSide(this, i2ps_, o3ps)

                                        fill = color[2]
                                        stroke = color[2]
                                        drawSide(this, i2ps, o0ps)
                                    }

                                    2 -> {
                                        fill = color[2]
                                        stroke = color[2]
                                        drawSide(this, i1ps_, o1ps)

                                        fill = color[1]
                                        stroke = color[1]
                                        drawSide(this, i1ps, o4ps)

                                        fill = color[2]
                                        stroke = color[2]
                                        drawSide(this, i2ps, o0ps)
                                    }

                                    else -> println("Unknown kind")
                                }
                            }
                        }
                    }
                }
            }
        }

        var rounds = 0
        extend {
            if (!s.hasAnimations()) {
                if (rounds > 0 && RECORD){
                    application.exit()
                } else {
                    s.offset = -0.075
                    s.rotated = 0.0
                    s.rotatedTiling = 0.0

                    // Animation should be a multiple of 3500 ms long for seamless loop
                    // see LineSegment.points function
                    s.apply {
                        delay(700)
                        complete()
                        animate("offset", 0.02, 800, Easing.QuartInOut)
                        complete()
                        animate("rotated", 120.0, 900, Easing.CubicInOut)
                        complete()
                        animate("rotatedTiling", 120.0, 1100, Easing.CubicInOut)
                        animate("offset", -0.075, 1000, Easing.QuartInOut)
                        complete()
                    }
                }
                rounds++
            }

            s.updateAnimation()
            comp.draw(drawer)
        }
    }
}
