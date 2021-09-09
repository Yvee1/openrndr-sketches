package indra

import org.openrndr.WindowMultisample
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.Bloom
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.*
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.sphereMesh
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.*
import org.openrndr.shape.*
import space.kscience.kmath.operations.*
import space.kscience.kmath.complex.*
import space.kscience.kmath.complex.ComplexField
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.extra.shapes.drawers.bezierPatch
import org.openrndr.extras.color.presets.CYAN
import org.openrndr.extras.color.presets.GOLD
import org.openrndr.extras.color.presets.PURPLE
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.transform
import useful.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val RECORD = false
fun main() = application {
    configure {
//        width = 2560
//        height = 1440
        width = 750
        height = 750
        windowResizable = false
        multisample = WindowMultisample.SampleCount(8)
        windowAlwaysOnTop = false
    }

    program {
//        println(sphereToComplex(complexToSphere(Complex(2.0, 5.0))))

        fun T(z: Complex, k: Complex) = ComplexField { k * z }
        fun R(z: Complex) = ComplexField { (z - 1) / (z + 1) }
        fun THat(z: Complex, k: Complex) = ComplexField { (z*(1 + k) + k - 1) / (z * (k - 1) + 1 + k) }

        val s = object: Animatable() {
            @DoubleParameter("n", 5.0, 10000.0)
            var n = 10000.0

            @IntParameter("Step divider", 1, 100)
//            var stepDivider = 10
            var stepDivider = 100

            @XYParameter("Start point 1", -1.0, 1.0, -1.0, 1.0)
            var start1 = Vector2(0.22, 0.0)

            @XYParameter("Start point 2", -1.0, 1.0, -1.0, 1.0)
            var start2 = Vector2(-0.22, 0.0)

            @XYParameter("k", -1.0, 1.0, -1.0, 1.0)
//            var k = Vector2(1.0, 0.4)
            var k = Vector2(0.63, 0.0)

//            @XYParameter("Start point", 0.28, 0.31, -0.05, 0.05, precision=5)
//            var start = Vector2(0.28, -0.06)

            @DoubleParameter("Animation duration", 0.1, 10.0, order=8)
            var duration = 10.0

            @BooleanParameter("Animate")
            var animate = RECORD

            @ColorParameter("Background color")
            var bg = ColorRGBa.BLACK

            @ColorParameter("Patch color 3")
//            var color = ColorRGBa.GOLD
            var color = ColorRGBa(1.61, 1.53, 0.0, 1.0)

            @DoubleParameter("r", 0.0, 1.0)
            var r = 0.5
        }

        if (!RECORD) {
            val gui = GUI()
            gui.doubleBind = true
            gui.add(s)
            extend(gui)

            extend(Screenshots()){
                multisample = BufferMultisample.SampleCount(32)
            }
        } else {
            extend(ScreenRecorder()){
                profile = GIFProfile()
                frameRate = 50
                maximumDuration = s.duration
                multisample = BufferMultisample.SampleCount(8)
            }
//            extend(TemporalBlur()){
////                jitter = 0.0
//                fps = 50.0
//                duration = 0.9
//                samples = 5
//            }
        }

        fun animate() {
            if (!s.hasAnimations()) {
//                s.n = 2.0
//                s.apply {
//                    val halfDuration = (s.duration/2.0 * 1000).toLong()
//                    ::n.animate(5000.0, halfDuration, Easing.CubicInOut)
//                    ::n.complete()
//                    ::n.animate(2.0, halfDuration, Easing.CubicInOut)
//                }
                s.k = Vector2(0.63,0.0)
                s.apply {
                    val halfDuration = (s.duration/2.0 * 1000).toLong()
                    ::k.animate(Vector2(0.63,0.56), halfDuration, Easing.CubicInOut)
                    ::k.complete()
                    ::k.animate(Vector2(0.63,0.0), halfDuration, Easing.CubicInOut)
                    ::k.complete()
                    ::k.animate(Vector2(0.63,-0.56), halfDuration, Easing.CubicInOut)
                    ::k.complete()
                    ::k.animate(Vector2(0.63,0.0), halfDuration, Easing.CubicInOut)
                }
            }
            s.updateAnimation()
//            val t = seconds / s.duration
//            s.k = Vector2(cos(t * 2 * PI + PI/2), (sin(2*t * 2 * PI + PI/2) + 1.0)/2.0) * s.r
//            s.start2 = -s.start1

        }

        extend {
            if (s.animate) animate()

            drawer.fontMap = loadFont("data/fonts/default.otf", 18.0)
            val k = ComplexField { s.k.toComplex().pow(1.0/s.stepDivider) }
            fun transformation(z: Complex) = THat(z, k)
            fun transformationInv(z: Complex) = THat(z, k.reciprocal)

            fun getSpiralPoints(start: Complex): List<Vector2> {
                var current = start.copy()
                val pointsBackward = List((s.n / 2).toInt()) {
                    val v = current.toVector2()
                    current = transformationInv(current)
                    v
                }
                current = transformation(start)
                val pointsForward = List((s.n / 2 - 1).toInt()) {
                    val v = current.toVector2()
                    current = transformation(current)
                    v
                }
                return pointsBackward.reversed() + pointsForward
            }

            fun drawDoubleSpiral(points1: List<Vector2>, points2: List<Vector2>) {
                val segments = points1.zip(points2) { pt1, pt2 -> Segment(pt1, pt2) }
                if (segments.size >= 4) {
                    val col = listOf(ColorRGBa.BLACK, ColorRGBa.GOLD, s.color, ColorRGBa.BLACK)
//                    val col = listOf(ColorRGBa.CYAN, ColorRGBa.PURPLE, ColorRGBa.RED, ColorRGBa.PINK)
                    val cols = listOf(col, col, col, col)
//                    val cols = List(4){ listOf(col[it], col[it], col[it], col[it]) }
                    val bps = segments.windowed(4, 3, false) { bezierPatch(it[0], it[1], it[2], it[3]).withColors(cols) }
                    bps.forEach { drawer.bezierPatch(it) }
                }
            }

            drawer.apply {
                clear(s.bg)

                val points1 = getSpiralPoints(s.start1.toComplex())
                val points2 = getSpiralPoints(s.start2.toComplex())

                stroke = null
                fill = ColorRGBa.PINK

                isolated {
                    translate(width/2.0, height/2.0)
                    scale(50.0)
                    drawDoubleSpiral(points1, points2)
                }
            }
        }
    }
}