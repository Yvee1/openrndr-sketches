package indra

import org.openrndr.WindowMultisample
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.XYParameter
import org.openrndr.extra.shapes.BezierPatch3D
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.meshGenerator
import org.openrndr.extras.meshgenerators.sphereMesh
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.*
import org.openrndr.shape.*
import space.kscience.kmath.operations.*
import space.kscience.kmath.complex.*
import space.kscience.kmath.complex.ComplexField
import useful.FPSDisplay
import useful.toVector3
import kotlin.math.pow
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.extra.shapes.drawers.bezierPatch


private const val RECORD = false
fun main() = applicationSynchronous {
    configure {
        width = 1000
        height = 1000
        windowResizable = true
        multisample = WindowMultisample.SampleCount(8)
    }

    program {
        fun T(z: Complex, k: Complex) = ComplexField { k * z }
        fun R(z: Complex) = ComplexField { (z - 1) / (z + 1) }
        fun complexToSphere(z: Complex) = Vector3(z.re, z.re * z.re + z.im * z.im, z.im) / (z.re * z.re + z.im * z.im + 1)
        fun THat(z: Complex, k: Complex) = ComplexField { (z*(1 + k) + k - 1) / (z * (k - 1) + 1 + k) }

        val s = object: Animatable() {
            @DoubleParameter("n", 5.0, 5000.0)
            var n = 250.0

            @IntParameter("Step divider", 1, 100)
            var stepDivider = 10

            @XYParameter("Start point", -1.0, 1.0, -1.0, 1.0)
            var start = Vector2(0.0, 0.0)

            @DoubleParameter("Animation duration", 0.1, 10.0, order=8)
            var duration = 6.0

            @BooleanParameter("Animate")
            var animate = RECORD
        }

        val sphere = sphereMesh(64, 64, 0.499)

        val cam = Orbital()
        extend(cam){
            eye = Vector3(x=-0.3987975414625309, y=0.6975846551992259, z=-1.1768437320876621)
            lookAt = Vector3(0.0, 0.5, 0.0)
            far = 2000.0
            fov = 50.0
        }

        if (!RECORD) {
            val gui = GUI()
            gui.doubleBind = true
            gui.add(s)
            extend(gui)

            extend(Screenshots()){
                multisample = BufferMultisample.SampleCount(8)
            }
        } else {
            extend(ScreenRecorder()){
                profile = GIFProfile()
                frameRate = 50
                maximumDuration = s.duration
                multisample = BufferMultisample.SampleCount(8)
            }
            extend(TemporalBlur()){
                jitter = 0.0
                fps = 50.0
                duration = 1.5
                samples = 50
            }
        }

        fun animate() {
            if (!s.hasAnimations()) {
                s.n = 2.0
                s.apply {
                    val halfDuration = (s.duration/2.0 * 1000).toLong()
                    ::n.animate(1750.0, halfDuration, Easing.CubicInOut)
                    ::n.complete()
                    ::n.animate(2.0, halfDuration, Easing.CubicInOut)
                }
            }
            s.updateAnimation()
        }

        extend {
            if (s.animate) animate()
            drawer.fontMap = loadFont("data/fonts/default.otf", 18.0)
            val k = ComplexField { Complex(1, 0.4).pow(1.0/s.stepDivider) }
            fun transformation(z: Complex) = T(z, k)
            fun transformationInv(z: Complex) = T(z, k.reciprocal)

            fun getSpiralPoints(start: Complex): List<Vector3> {
                var current = start.copy()
                val pointsBackward = List((s.n / 2).toInt()) {
                    val v = current.toVector3()
                    current = transformationInv(current)
                    v
                }
                current = transformation(start)
                val pointsForward = List((s.n / 2 - 1).toInt()) {
                    val v = current.toVector3()
                    current = transformation(current)
                    v
                }
                val points = pointsBackward.reversed() + pointsForward
                val spherePoints = points.map { complexToSphere(Complex(it.x, it.z)) }
                return spherePoints
            }
            ShapeContour
            drawer.apply {
                val points1 = getSpiralPoints(Complex(0.8, 0))
                val points2 = getSpiralPoints(Complex(1, 0))

                stroke = null
                fill = ColorRGBa.PINK
                val segments = points1.zip(points2) { pt1, pt2 -> Segment3D(pt1, pt2) }

                if (segments.size >= 4) {
                    val col = listOf(ColorRGBa.PINK, ColorRGBa.RED, ColorRGBa.BLUE, ColorRGBa.PINK)
                    val cols = listOf(col, col, col, col)
                    val bps = segments.windowed(4, 3, false) { bezierPatch(it[0], it[1], it[2], it[3]).withColors(cols) }
                    bps.forEach { bezierPatch(it) }
                }

                segments(segments)
            }
        }
    }
}
