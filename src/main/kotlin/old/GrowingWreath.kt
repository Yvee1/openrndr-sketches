package old

import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSVa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blend.Multiply
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.noise.*
import org.openrndr.extra.noise.filters.CellNoise
import org.openrndr.extra.noise.filters.SimplexNoise3D
import org.openrndr.extra.olive.Reloadable
import org.openrndr.extra.shadestyles.radialGradient
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriterProfile
import org.openrndr.math.*
import org.openrndr.math.Polar.Companion.fromVector
import org.openrndr.math.Vector2.Companion.fromPolar
import org.openrndr.shape.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
//import kotlin.random.Random

/**
 * Inspired by: https://codepen.io/pelletierauger/full/xOqNYa/
 */

fun main() = application {
    configure {
        width = 1000
        height = 1000
        position = IntVector2(1400, 200)
    }

    program {
        val w = width.toDouble()
        val h = height.toDouble()
        val center = Vector2(0.0, 0.0)

        val gui = GUI()

        val s = object : Reloadable() {
            @IntParameter("Branches", 1, 10000)
            var n: Int = 4500

            @DoubleParameter("Mean radius", 1.0, 400.0)
            var r: Double = 275.0

            @DoubleParameter("Radius margin", 0.0, 400.0)
            var margin: Double = 40.0

            @DoubleParameter("Growing deviation", 0.1, 45.0)
            var delta: Double = 10.0

            @DoubleParameter("Branching probability", 0.0, 1.0)
            var p: Double = 0.04

            @ColorParameter("Background")
            var bg: ColorRGBa = ColorRGBa(0.0, 0.0, 0.0)

            @IntParameter("Length", 1, 100)
            var length: Int = 9

            @DoubleParameter("Segment length", 1.0, 10.0)
            var l: Double = 4.0

            @DoubleParameter("Stroke weight", 0.1, 5.0)
            var thick: Double = 0.34

            @DoubleParameter("Right branching preference", -1.0, 1.0)
            var rightPref: Double = 0.0
        }
        gui.add(s, "Settings")

        val bounds = Rectangle(0.0, 0.0, w, h)

//        val comp = drawComposition {
//
//        }

//        val root = Branch(center, Vector2(0.0, -1.0))
        var roots: ArrayList<Branch> = ArrayList()

        var circle = Circle(center, s.r)
        var circlePoints = circle.contour.equidistantPositions(s.n+1).subList(1, s.n+1)

        for (point in circlePoints){
            val dir = point.rotate(110.0).normalized
            val root = Branch(point + point.normalized.rotate(90.0) * Math.random() * s.margin, dir, s.delta, s.p, l=s.l, rightPref=s.rightPref, thick=s.thick)
            roots.add(root)
            root.growRepeatedly(30)
        }

//        extend(gui)

        val res = 1000
        val n = 9
        val extra = res/10
        var points: Array<ArrayList<Vector2>> = Array(n) { ArrayList() }
        for (j in 0 until n) {
            for (i in -extra until res+extra) {
                val t = i.toDouble() / res * PI / n + PI / 2 / n + j * 2 * PI/n
                val r = 0.98*s.r + 1.15 * s.margin * sin(n * t)
                points[j].add(Vector2(r * cos(t), r * sin(t)))
            }
        }

//        gui.onChange { _, _ ->
            roots = ArrayList()

            circle = Circle(center, s.r)
            circlePoints = circle.contour.equidistantPositions(s.n+1).subList(1, s.n+1)

            for (point in circlePoints){
                val dir = point.rotate(110.0).normalized
                val root = Branch(point + point.normalized * Random.double(-s.margin, s.margin), dir, s.delta, s.p, l=s.l, rightPref=s.rightPref, thick=s.thick)
//                println(point.normalized * Random.double(-s.margin, s.margin))
                roots.add(root)
                root.growRepeatedly(s.length)
            }

            points = Array(n) { ArrayList() }
            for (j in 0 until n) {
                for (i in -extra until res+extra) {
                    val t = i.toDouble() / res * PI / n + PI / 2 / n + j * 2 * PI/n
                    val r = 0.98*s.r + 1.15 * s.margin * sin(n * t)
                    points[j].add(Vector2(r * cos(t), r * sin(t)))
                }
            }
//        }

//        val sn = SimplexNoise3D()
//        sn.seed = Vector3.ZERO
//        sn.scale = Vector3.ONE * 64.0
//        sn.octaves = 4
//        sn.premultipliedAlpha = true
//        val cb = colorBuffer(width, height)
        extend(ScreenRecorder()) {
            profile = GIFProfile()
            frameRate = 30
        }
        extend(Screenshots()){
            scale = 1.0
        }
        extend {
            val c = compose {
                layer {
                    draw {
                        drawer.clear(s.bg)
//                        sn.apply(emptyArray(), cb)
//                        drawer.drawStyle.blendMode = BlendMode.MULTIPLY
//                        drawer.image(cb)
//                        drawer.drawStyle.blendMode = BlendMode.OVER
                    }
                }

                layer{
                    draw{
                        drawer.translate(w / 2, h / 2)
                        for (root in roots) {
                            root.animate()
                            root.drawRecursively(drawer)
                        }

                        val sw = 8.0
                        for (j in 0 until n) {
                            val c = contour {
                                moveTo(points[j][0])

                                for (i in 1 until points[j].size - 1) {
                                    val normal = (points[j][i] - points[j][i - 1]).normalized.rotate(90.0)
                                    lineTo(points[j][i] + normal * sw / 2.0)
                                }

                                for (k in 1 until points[j].size) {
                                    val i = points[j].size - k
                                    val normal = (points[j][i] - points[j][i - 1]).normalized.rotate(90.0)
                                    lineTo(points[j][i] - normal * sw / 2.0)
                                }

                                close()
                            }
                            drawer.shadeStyle = radialGradient(ColorRGBa.RED, s.bg, length = 0.75)
                            drawer.stroke = null
                            drawer.fill = ColorRGBa.WHITE
                            drawer.contour(c)

                            drawer.isolated {
                                drawer.rotate(360.0 / n / 4)
                                drawer.shadeStyle = radialGradient(ColorRGBa.WHITE, s.bg, length = 0.75)
                                drawer.stroke = null
                                drawer.fill = ColorRGBa.WHITE
                                drawer.contour(c)
                            }

                            drawer.isolated {
                                drawer.rotate(360.0 / n / 8)
                                drawer.shadeStyle = radialGradient(ColorRGBa.YELLOW, s.bg, length = 0.75)
                                drawer.stroke = null
                                drawer.fill = ColorRGBa.WHITE
                                drawer.contour(c)
                            }
                        }
                    }
                }
            }
            c.draw(drawer)

            if (frameCount > 120){
                application.exit()
            }
        }
    }
}

class Branch(var pos: Vector2, var dir: Vector2, val delta: Double, val p: Double, val i: Int = 0, val rightPref: Double = 1.0, val l: Double = 3.0, val thick: Double = 2.0, val hue: Double = Random.gaussian(120.0, 5.0)) {
    var directChild: Branch? = null
    var adjacentChild: Branch? = null

    fun draw(drawer: Drawer) {
        drawer.fill = ColorRGBa.GREEN
        drawer.stroke = ColorXSVa(hue, 0.9, 1.0, 0.25).toRGBa()
        drawer.strokeWeight = thick
        drawer.lineSegment(pos, pos+dir*l)

//        drawer.fill = ColorRGBa.RED
//        drawer.stroke = null
//        drawer.circle(pos, 7.0)
    }

    fun drawRecursively(drawer: Drawer){
        draw(drawer)
        directChild?.drawRecursively(drawer)
        adjacentChild?.drawRecursively(drawer)
    }

    fun grow() {
        val newDir = shake(dir)
        val newPos = pos + dir*l
        directChild = Branch(newPos, newDir, delta, p, i+1, rightPref, l, thick, hue+Random.gaussian(3.0, 1.0))

        if (Math.random() < p && i > 1){
            val right = Random.bool(0.5)
            val sign = if (right) 1.0 else -1.0
            val newDir = dir.addAngle(sign*Random.gaussian(30.0, 10.0))
            adjacentChild = Branch(pos, newDir, delta, p/2, i, rightPref, l, thick, hue)
            adjacentChild?.growRepeatedly(Random.int(1, 5))
        }
    }

    fun growRepeatedly(n: Int) {
        if (n > 0) {
            grow()
            directChild?.growRepeatedly(n - 1)
        }
    }

    fun shake(vec: Vector2) : Vector2 {
//        return Vector2.fromPolar((fromVector(vec) + Polar(Random.nextDouble(-delta, delta), 0.0)).makeSafer())
        return vec.addAngle(Random.gaussian(rightPref*delta, delta))
    }

    fun animate() {
        dir = dir.addAngle(5*Random.simplex(pos.x, pos.y))
        directChild?.pos = pos + dir * l
        adjacentChild?.pos = pos
        directChild?.animate()
        adjacentChild?.animate()
    }
}

private fun Polar.makeSafer() = Polar(
    mod(theta, 360.0),
    radius
)

private fun Vector2.addAngle(angle: Double) = fromPolar((fromVector(this) + Polar(angle, 0.0)).makeSafer())