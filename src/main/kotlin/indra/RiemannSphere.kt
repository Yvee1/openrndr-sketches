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
import org.openrndr.extras.color.presets.PURPLE
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.transform
import useful.toComplex
import useful.toVector3
import useful.complexToSphere
import useful.sphereToComplex
import kotlin.math.cos
import kotlin.math.sin

private const val RECORD = false
fun main() = application {
    configure {
//        width = 2560
//        height = 1440
        width = 500
        height = 500
        windowResizable = true
        multisample = WindowMultisample.SampleCount(8)
    }

    program {
//        println(sphereToComplex(complexToSphere(Complex(2.0, 5.0))))

        fun T(z: Complex, k: Complex) = ComplexField { k * z }
        fun R(z: Complex) = ComplexField { (z - 1) / (z + 1) }
        fun THat(z: Complex, k: Complex) = ComplexField { (z*(1 + k) + k - 1) / (z * (k - 1) + 1 + k) }

        val s = object: Animatable() {
            @DoubleParameter("n", 5.0, 10000.0)
            var n = 1000.0

            @IntParameter("Step divider", 1, 100)
//            var stepDivider = 10
            var stepDivider = 5

            @XYParameter("Start point", -1.0, 1.0, -1.0, 1.0)
            var start = Vector2(0.0, 0.0)

//            @XYParameter("Start point", 0.28, 0.31, -0.05, 0.05, precision=5)
//            var start = Vector2(0.28, -0.06)

            @DoubleParameter("Animation duration", 0.1, 10.0, order=8)
            var duration = 6.0

            @BooleanParameter("Animate")
            var animate = RECORD

            @ColorParameter("Background color")
            var bg = ColorRGBa.PINK
        }


        val sphere = sphereMesh(64, 64, 0.495)

        val cam = Orbital()
        extend(cam){
//            eye = Vector3(x=-0.3987975414625309, y=0.6975846551992259, z=-1.1768437320876621)
//            lookAt = Vector3(0.0, 0.5, 0.0)

            eye = Vector3.UNIT_Y * 5.0
            lookAt = Vector3(0.0, 0.0, 0.0)
            far = 2000.0
            fov = 50.0
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
                    ::n.animate(5000.0, halfDuration, Easing.CubicInOut)
                    ::n.complete()
                    ::n.animate(2.0, halfDuration, Easing.CubicInOut)
                }
            }
            s.updateAnimation()
        }

        extend {
//            cam.camera.spherical.
//            print("${cam.camera.spherical.cartesian}, ${cam}\r")
            if (s.animate) animate()

            drawer.fontMap = loadFont("data/fonts/default.otf", 18.0)
//            val start = Complex(0.0, 0.0)
            val k = ComplexField { Complex(1, 0.4).pow(1.0/s.stepDivider) }
            fun transformation(z: Complex) = THat(z, k)
            fun transformationInv(z: Complex) = THat(z, k.reciprocal)

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
                return pointsBackward.reversed() + pointsForward
            }

            fun drawDoubleSpiral(points1: List<Vector3>, points2: List<Vector3>) {
                val segments = points1.zip(points2) { pt1, pt2 -> Segment3D(pt1, pt2) }
                if (segments.size >= 4) {
                    val col = listOf(ColorRGBa.PINK, ColorRGBa.RED, ColorRGBa.BLUE, ColorRGBa.PINK)
//                    val col = listOf(ColorRGBa.CYAN, ColorRGBa.PURPLE, ColorRGBa.RED, ColorRGBa.PINK)
                    val cols = listOf(col, col, col, col)
//                    val cols = List(4){ listOf(col[it], col[it], col[it], col[it]) }
                    val bps = segments.windowed(4, 3, false) { bezierPatch(it[0], it[1], it[2], it[3]).withColors(cols) }
                    bps.forEach { drawer.bezierPatch(it) }
                }
            }

            drawer.apply {
                clear(s.bg)
//                isolated {
//                    translate(0.0, 0.5, 0.0)
//                    vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
//                }
                val points1 = getSpiralPoints(s.start.toComplex())
                val points2 = getSpiralPoints(Complex(0, 0))

                val m4 = transform {
                    translate(0.0, 0.5, 0.0)
                    rotate(Vector3.UNIT_X, seconds / s.duration * 360.0)
                    translate(0.0, -0.5, 0.0)
                }

                val spherePoints1 = points1.map { m4 * complexToSphere(Complex(it.x, it.z)) }
                val spherePoints2 = points2.map { m4 * complexToSphere(Complex(it.x, it.z)) }

//                val tPoints1 = spherePoints1.map { sphereToComplex(it).toVector3() }
//                val tPoints2 = spherePoints2.map { sphereToComplex(it).toVector3() }
//                println(tPoints1)

//                strokeWeight = 50.0
//                lineStrip(points1)
//                lineStrip(points2)

                stroke = null
                fill = ColorRGBa.PINK
//                rotateZ(seconds * 5.0) *
                drawDoubleSpiral(points1, points2)
                drawDoubleSpiral(spherePoints1, spherePoints2)
            }
        }
    }
}

//fun CatmullRom3.toSegment3D(): Segment3D {
//    val d1a2 = (p1 - p0).length.pow(2 * alpha)
//    val d2a2 = (p2 - p1).length.pow(2 * alpha)
//    val d3a2 = (p3 - p2).length.pow(2 * alpha)
//    val d1a = (p1 - p0).length.pow(alpha)
//    val d2a = (p2 - p1).length.pow(alpha)
//    val d3a = (p3 - p2).length.pow(alpha)
//
//    val b0 = p1
//    val b1 = (p2 * d1a2 - p0 * d2a2 + p1 * (2 * d1a2 + 3 * d1a * d2a + d2a2)) / (3 * d1a * (d1a + d2a))
//    val b2 = (p1 * d3a2 - p3 * d2a2 + p2 * (2 * d3a2 + 3 * d3a * d2a + d2a2)) / (3 * d3a * (d3a + d2a))
//    val b3 = p2
//
//    return Segment3D(b0, b1, b2, b3)
//}

fun Matrix33.Companion.rotateZ(angle: Double): Matrix33 {
    val r = angle.asRadians
    val cr = cos(r)
    val sr = sin(r)
    return Matrix33(
        cr, -sr, 0.0,
        sr, cr, 0.0,
        0.0, 0.0, 1.0)
}

fun Matrix33.Companion.rotate(axis: Vector3, angle: Double): Matrix33 {

    val r = angle.asRadians
    val cosa = cos(r)
    val sina = sin(r)
    val _axis = axis.normalized

    return Matrix33(

        cosa + (1 - cosa) * _axis.x * _axis.x,
        (1 - cosa) * _axis.x * _axis.y - _axis.z * sina,
        (1 - cosa) * _axis.x * _axis.z + _axis.y * sina,

        (1 - cosa) * _axis.x * _axis.y + _axis.z * sina,
        cosa + (1 - cosa) * _axis.y * _axis.y,
        (1 - cosa) * _axis.y * _axis.z - _axis.x * sina,

        (1 - cosa) * _axis.x * _axis.z - _axis.y * sina,
        (1 - cosa) * _axis.y * _axis.z + _axis.x * sina,
        cosa + (1 - cosa) * _axis.z * _axis.z)

}

operator fun Matrix44.times(v: Vector3): Vector3 {
    val v4 = this * Vector4(v.x, v.y, v.z, 1.0)
    return Vector3(v4.x, v4.y, v4.z)
}