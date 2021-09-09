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
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.simplex
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
import org.openrndr.extras.color.spaces.toHSLUVa
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.transform
import useful.*
import java.io.File
import kotlin.math.*

private const val RECORD = false
fun main() = application {
    configure {
//        width = 2560
//        height = 1440
        width = 700
        height = 1000
        windowResizable = true
        multisample = WindowMultisample.SampleCount(8)
        windowAlwaysOnTop = false
    }

    program {
//        println(sphereToComplex(complexToSphere(Complex(2.0, 5.0))))

        fun T(z: Complex, k: Complex) = ComplexField { k * z }
        fun R(z: Complex) = ComplexField { (z - 1) / (z + 1) }
        fun RInv(z: Complex) = ComplexField { (z + 1) / (z - 1) }
        fun THat(z: Complex, k: Complex): Complex {
            return ComplexField {
                val denom = (z * (k - 1) + 1 + k)
                val denomLength = denom.r2()
                if (denomLength < 0.0001 || denomLength > 1000000) Complex(0, 0) else (z * (1 + k) + k - 1) / denom
            }
        }

        val s = object: Animatable() {
            @DoubleParameter("n", 5.0, 10000.0)
            var n = 2000.0

            @IntParameter("Step divider", 1, 100)
//            var stepDivider = 10
            var stepDivider = 1

            @XYParameter("Start point 1", -1.0, 1.0, -1.0, 1.0)
            var start1 = Vector2(0.07, 0.0)

            @XYParameter("Start point 2", -1.0, 1.0, -1.0, 1.0)
            var start2 = Vector2(-0.07, 0.0)

            @XYParameter("k", -1.0, 1.0, -1.0, 1.0)
//            var k = Vector2(1.0, 0.4)
            var k = Vector2(0.82, 0.33)

//            @XYParameter("Start point", 0.28, 0.31, -0.05, 0.05, precision=5)
//            var start = Vector2(0.28, -0.06)

            @DoubleParameter("Animation duration", 0.1, 200.0, order=8)
            var duration = 2.0

            @BooleanParameter("Animate")
            var animate = true

            @ColorParameter("Background color")
            var bg = ColorRGBa.BLACK

            @ColorParameter("Patch color 3")
//            var color = ColorRGBa.GOLD
            var color = ColorRGBa(1.61, 1.53, 0.0, 1.0)

            @DoubleParameter("r", 0.0, 10.0)
            var r = 0.683

            @IntParameter("period", 1, 50)
            var period = 9

            @DoubleParameter("Scale", 0.0, 1000.0)
            var scale = 140.0

            @DoubleParameter("Noise scale X", 0.0, 500.0)
            var noiseScaleX = 31.1

            @DoubleParameter("Noise scale Y", 0.0, 2.0)
            var noiseScaleY = 0.050

            @DoubleParameter("Position dependent", 0.0, 100.0)
            var posDependent = 14.3

            @DoubleParameter("Easing k", 1.0, 5.0)
            var easingK = 3.0

            @DoubleParameter("Easing k", 1.0, 5.0)
            var circleRadius = 1.5
        }

        val bloom = Bloom()
        val colorCorrection = ColorCorrection()


        val gui = GUI()
        gui.add(s)
        gui.add(bloom)
        gui.add(colorCorrection)
        gui.loadParameters(File("gui-parameters/DoubleSpiral3 final bright2.json"))
        gui.doubleBind = true

        if (!RECORD) {
            extend(gui)
            extend(FPSDisplay()){
                textColor = ColorRGBa.WHITE
            }
            extend(Screenshots()){
//                multisample = BufferMultisample.SampleCount(32)
            }
        } else {
            extend(ScreenRecorder()){
                profile = GIFProfile()
                frameRate = 50
                maximumDuration = s.duration
//                multisample = BufferMultisample.SampleCount(8)
            }
//            extend(TemporalBlur()){
////                jitter = 0.0
//                fps = 50.0
//                duration = 0.9
//                samples = 5
//            }
        }

        val k = ComplexField { s.k.toComplex().pow(1.0/s.stepDivider) }

        fun transformation(z: Complex) = THat(z, k)
        //            fun transformation(z: Complex) = R(T(RInv(z), k))
//            fun transformationInv(z: Complex) = R(T(RInv(z), k.reciprocal))
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

        val points1 = getSpiralPoints(s.start1.toComplex())
        val points2 = getSpiralPoints(s.start2.toComplex())

        val n = 5
        val contours = List(n){
            val alpha = it / (n.toDouble() - 1)
            CatmullRomChain2(points1.zip(points2) { a, b -> (1- alpha) * a + alpha * b } ).toContour()
        }

        val K = 5000
        var pointPositions = MutableList(n) {
            List(K) {
                contours[0].position(it / K.toDouble())
            }
        }

        var pointColors = MutableList(n) {
            MutableList(K) {
                ColorRGBa.WHITE
            }
        }

//        val s1 = contour1.segments
//        val s2 = contour2.segments.reversed()
//        val contourMerged = ShapeContour.fromSegments(s1 + listOf(Segment(s1.last().end, s2.first().start)) + s2, true, distanceTolerance = 1.0)
//        contour((contour1 + contour2.reversed).close)

        fun thickness(ut: Double) = (contours.first().equidistantPosition(ut) - contours.last().equidistantPosition(ut)).length

        val cThickness = List(K) {
            val ut = it / K.toDouble()
            thickness(ut)
        }

        val cPositions = List(n) { i ->
            List(K) {
                val ut = it / K.toDouble()
                contours[i].equidistantPosition(ut)
            }
        }

        val cNormals = List(n) { i ->
            List(K) {
                val ut = it / K.toDouble()
                contours[i].equidistantNormal(ut)
            }
        }

        val comp = compose {
            draw {
                drawer.circles {
                    for (i in 0 until n){
                        pointPositions[i].forEachIndexed { j, v ->
                            fill = pointColors[i][j]
                            circle(v, s.circleRadius / s.scale)
                        }
                    }
                }
            }
            post(colorCorrection)
            post(bloom)
        }

        extend {
            fun animate() {
//            if (!s.hasAnimations()) {
////                s.k = Vector2(0.63,0.0)
////                s.apply {
////                    val halfDuration = (s.duration/2.0 * 1000).toLong()
////                    ::k.animate(Vector2(0.63,0.56), halfDuration, Easing.CubicInOut)
////                    ::k.complete()
//                }
//            }
//            s.updateAnimation()
//            s.start2 = -s.start1
                val t = seconds/s.duration % 1

                for (i in 0 until n) {
                    val seed = i * 29 + 30

//                    fun offset(p: Double) = s.period * p.pow(3)//sin(p*PI)
//                    fun offset(p: Double): Double {
//                        if (p < 0.0 || p > 1.0) println("!!!: ${p}")
//                        if (sin(p*PI) < 0.0 || sin(p*PI) > 1.0) println("*!!!*: ${p}: ${sin(p*PI)}")
//                        return s.period * sin(p*PI)
//                    }

                    fun easingK(x: Double, k: Double) = if (x < 0.5) 2.0.pow(k-1)*x.pow(k) else 1 - (-2 * x + 2).pow(k) / 2
                    fun easing(x: Double) = easingK(x, s.easingK)

                    fun offset(p: Double): Double {
//                        return s.period * (((2*(p-0.5)).pow(2)))
                        return s.period * easing(p)
                    }

                    fun F(p: Double, seed: Int): Double {
                        val r = s.r
//                        print("${p}: ${offset(p)}, ")
                        return simplex(
                            seed,
                            r * cos(2 * PI * (offset(p) + t)),
                            r * sin(2 * PI * (offset(p) + t)),
                            s.posDependent * p
                        )
//                        return simplex(
//                            seed,
//                            r * cos(2 * PI * (p + t)),
//                            r * sin(2 * PI * (p + t)),
//                            s.posDependent * p
//                        )
//                return simplex(1, r*(p + t))
                    }

//                fun offset(p: Double) = 3.0 * p

//                    val hLine = contour {
//                        moveTo(-2.0, 0.0)
//                        lineTo(2.0, 0.0)
//                    }
                    //https://stackoverflow.com/questions/4200224/random-noise-functions-for-glsl
                    fun random(seed: Vector2): Double {
                        return (sin(seed.dot(Vector2(12.9898, 78.233))) * 43758.5453).mod(1.0)
                    }

                    pointPositions[i] = List(K) {
                        val ut = it / K.toDouble()// + t/K.toDouble()
//                val dy = F(offset(ut), ut)
//                    val dy = F(offset(ut) - t, ut) * 0.1
                        val dx = F(ut, seed) * s.noiseScaleX
                        val dy = F(ut, seed * 2) * s.noiseScaleY

//                        print("${ut}: ${offset(ut)}, ")
                        val nvec = cNormals[i][it]
//                    contours[0].position(ut) + dy * contours[0].normal(ut) * thickness(ut)
//                        cPositions[i][it] + dx * nvec.perpendicular() + dy * nvec * thickness(ut)
//                        cPositions[i][(it + dx.toInt()).mod(K)] + dy * nvec * cThickness[it]

//                        pointColors[i][it] = ColorRGBa(1.0, 1.0, 1.0, 0.4)


//                        pointColors[i][it] = ColorRGBa.RED.mix(ColorRGBa.BLUE, 0.5*sin(ut*2*PI*50) + 0.5)

                        val c1 = ColorRGBa(1.0, 0.5, 0.0, 0.4)//.toHSLUVa()
                        val c2 = ColorRGBa(0.0, 0.5, 1.0, 0.4)//.toHSLUVa()
//                        pointColors[i][it] = if (i % 2 == 0) c1 else c2//c1.mix(c2, random(Vector2(ut*7.0, ut*7.0)))//.toRGBa()
//                        pointColors[i][it] = c1.mix(c2, (offset(ut) + t).mod(1.0))//.toRGBa()
//                        pointColors[i][it] = c1.mix(c2, random(Vector2(offset(ut) + t, 0.0)))//.roundToInt().toDouble())
                        pointColors[i][it] = c1.mix(c2, (F(ut, seed*3) + 1)/2)
                        cPositions[i][(it + dx.toInt()).mod(K)] + dy * nvec * cThickness[it]
//                        hLine.position((ut + dx).mod(1.0)) + dy * hLine.normal(ut) //* cThickness[it]
//                    hLine.position(ut) + dy * hLine.normal(ut) * thickness(ut)
                    }
                }
            }

            if (s.animate) animate()

            drawer.fontMap = loadFont("data/fonts/default.otf", 18.0)

            drawer.apply {
                clear(s.bg)

                stroke = null
                fill = ColorRGBa.PINK

                isolated {
                    translate(width/2.0, height/2.0)
                    rotate(90.0)
                    scale(s.scale)
                    strokeWeight = strokeWeight / s.scale
//                    drawDoubleSpiral(points1, points2)

                    stroke = null
                    fill = ColorRGBa.WHITE.opacify(0.4)
//                    circles(pointPositions.flatten(), 1.5/s.scale)
                    comp.draw(this)
                }
            }
        }
    }
}

fun ShapeContour.equidistantPosition(ut: Double) = position(tForLength(ut * length))
fun ShapeContour.equidistantNormal(ut: Double) = normal(tForLength(ut * length))