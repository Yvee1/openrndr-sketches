import org.openrndr.animatable.Animatable
import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.Drawer
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.Random.gaussian
import org.openrndr.extra.noise.uniforms
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.extra.shapes.operators.bulgeSegments
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.transforms.translate
import org.openrndr.shape.*
import kotlin.math.*

fun main() = applicationSynchronous {
    configure {
        width = 1000
        height = 1000
    }

    program {
        class MetaballPair(c1: Circle, c2: Circle, var v: Double = 0.5, var handleSize: Double = 2.4) {
            var circle1: Circle
            var circle2: Circle
            var contours: List<ShapeContour>
            val padding = 7.0

            var bonded = false
            var animating = false

            var animatedCircle1: (Double) -> ShapeContour
            var animatedCircle2: (Double) -> ShapeContour

            var timeOfLastSeparation: Double = 0.0

            init {
                // Make circle1 the larger circle
                val bigger = c1.radius > c2.radius
                circle1 = if (bigger) c1 else c2
                circle2 = if (bigger) c2 else c1

                if (c1.radius <= 0.0){
                    if (c2.radius <= 0.0){
                        contours = listOf()
                    } else {
                        contours = listOf(c2.contour)
                    }
                } else if (c2.radius <= 0.0) {
                    contours = listOf(c1.contour)
                } else {
                    contours = listOf(c1.contour, c2.contour)
                }

                animatedCircle1 = { _ -> circle1.copy(radius=circle1.radius*0.99).contour.transform(Matrix44.translate(-circle1.center.x, -circle1.center.y, 0.0)) }
                animatedCircle2 = { _ -> circle2.copy(radius=circle2.radius*0.99).contour.transform(Matrix44.translate(-circle2.center.x, -circle2.center.y, 0.0)) }
            }

            fun update(t: Double) {
                if(bonded || (circle2.center - circle1.center).length <= circle1.radius + circle2.radius + padding) {
                    calculateContours(t)
                } else {
                    animateCircles(t)
                }
            }

            fun easing(t: Double): Double {
                val c4 = (2 * PI) / 3

                return if (t <= 0.0) 0.0 else if (t >= 1.0) 1.0 else 2.0.pow(-10*t) * sin((t*10 - 0.75) * c4) + 1
            }

            fun animateCircles(t: Double) {
                contours = listOf(animatedCircle1(easing(t - timeOfLastSeparation)).transform(Matrix44.translate(circle1.center.x, circle1.center.y, 0.0)),
                    animatedCircle2(easing(t - timeOfLastSeparation)).transform(Matrix44.translate(circle2.center.x, circle2.center.y, 0.0)))
            }

            fun calculateContours(t: Double){
                val diff = circle2.center - circle1.center
                val d = diff.length
                if (d <= circle1.radius - circle2.radius){
                    contours = listOf(circle1.copy(radius=0.99*circle1.radius).contour)
                    return
                }

                val totalRadius = circle1.radius + circle2.radius
                val maxSpread1 = acos((circle1.radius - circle2.radius)/d)
                val maxSpread2 = PI - maxSpread1

                val u1 = if (d >= totalRadius) 0.0 else acos((circle1.radius.pow(2) + d.pow(2) - circle2.radius.pow(2)) / (2 * circle1.radius * d))
                val u2 = if (d >= totalRadius) 0.0 else acos((circle2.radius.pow(2) + d.pow(2) - circle1.radius.pow(2)) / (2 * circle2.radius * d))

                val spread1 = u1 + (maxSpread1 - u1) * v
                val spread2 = PI - (u2 + (maxSpread2 - u2) * v)

                val angleBetweenCenters = atan2(diff.y, diff.x)
                val angle11 = angleBetweenCenters + spread1
                val angle12 = angleBetweenCenters - spread1
                val angle21 = angleBetweenCenters + spread2
                val angle22 = angleBetweenCenters - spread2

                val p11 = getVector(circle1.center, angle11, circle1.radius)
                val p12 = getVector(circle1.center, angle12, circle1.radius)
                val p21 = getVector(circle2.center, angle21, circle2.radius)
                val p22 = getVector(circle2.center, angle22, circle2.radius)

                val d2Base = min(v * handleSize, p11.distanceTo(p21) / totalRadius)
                val d2 = d2Base * min(1.0, (d*2) / totalRadius)

                val r1 = circle1.radius * d2
                val r2 = circle2.radius * d2

                val h11 = getVector(p11, angle11 - PI/2, r1)
                val h12 = getVector(p12, angle12 + PI/2, r1)
                val h21 = getVector(p21, angle21 + PI/2, r2)
                val h22 = getVector(p22, angle22 - PI/2, r2)

                val curve1 = contour {
                    moveTo(p11)
                    curveTo(h11, h21, p21)
                }

                val curve2 = contour {
                    moveTo(p22)
                    curveTo(h22, h12, p12)
                }

                val intersections = curve1.intersections(curve2)
//                if (!bonded && d > totalRadius + padding) {
//                    bonded = false
//                    contours = listOf(circle1.contour, circle2.contour)
//                    return
//                }
                if (intersections.isNotEmpty() && bonded){
                    bonded = false
                    contours = listOf(circle1.contour, circle2.contour)
//                    print(intersections.first().a.contourT.toString() + "\r")
//                    test = curve1.sub(0.0, intersections.first().b.contourT)
//                    val part1 = curve1.sub(0.0, intersections.first().b.contourT).segments
//                    val part2 = curve2.sub(intersections.first().a.contourT, 1.0).segments


                    animating = true
                    timeOfLastSeparation = t

                    val oldCircle1 = circle1.copy()
                    val oldCircle2 = circle2.copy()

                    animatedCircle1 = { t: Double ->
                        val start1 = curve1.sub(0.0, intersections.first().b.contourT).segments.first()
                        val start2 = curve2.sub(intersections.first().a.contourT, 1.0).segments.first()

                        var end = contour {
                            moveTo(p11)
                            arcTo(circle1.radius, circle1.radius, 0.0, false, false, p12)
                        }.segments

                        if (end.size == 1){
                            end = end.first().split(0.5).toList()
                        }

//                        println(end)

                        val midPt = start1.end * (1-t) + end.first().end * t
                        val midControl1 = start1.control[0] * (1-t) + end.first().control[0] * t
                        val midControl2 = start1.control[1] * (1-t) + end.first().control[1] * t

                        val endControl1 = start2.control[0] * (1-t) + end[1].control[0] * t
                        val endControl2 = start2.control[1] * (1-t) + end[1].control[1] * t

                        contour {
                            moveTo(p11)
                            curveTo(midControl1, midControl2, midPt)
                            curveTo(endControl1, endControl2, p12)
                            arcTo(circle1.radius, circle1.radius, 0.0, true, false, p11)
                            close()
                        }.transform(Matrix44.translate(-oldCircle1.center.x, -oldCircle1.center.y, 0.0))
                    }

                    animatedCircle2 = { t: Double ->
                        val start1 = curve1.sub(intersections.first().b.contourT, 1.0).reversed.segments.first()
                        val start2 = curve2.sub(0.0, intersections.first().a.contourT).reversed.segments.first()

                        val t = t.clamp(-0.05, 1.05)

                        var end = contour {
                            moveTo(p21)
                            arcTo(circle2.radius, circle2.radius, 0.0, false, true, p22)
                        }.segments

                        if (end.size == 1){
                            end = end.first().split(0.5).toList()
                        }

                        val midPt = start1.end * (1-t) + end.first().end * t
                        val midControl1 = start1.control[0] * (1-t) + end.first().control[0] * t
                        val midControl2 = start1.control[1] * (1-t) + end.first().control[1] * t

                        val endControl1 = start2.control[0] * (1-t) + end[1].control[0] * t
                        val endControl2 = start2.control[1] * (1-t) + end[1].control[1] * t

                        contour {
                            moveTo(p21)
                            curveTo(midControl1, midControl2, midPt)
                            curveTo(endControl1, endControl2, p22)
                            arcTo(circle2.radius, circle2.radius, 0.0, true, true, p21)
                            close()
                        }.transform(Matrix44.translate(-oldCircle2.center.x, -oldCircle2.center.y, 0.0))
                    }


//                    test = ShapeContour(part1 + listOf(Segment(part1.last().end, part2.first().start)) + part2, false)
//                    test = approximateArc(p21, p22, circle2.center)
//                    test?.let { println(it.segments) }
//                    println(part1)

                    return
                }
                if (d <= totalRadius + padding) {
                    bonded = true
                }

                contours = listOf(contour {
                    moveTo(p12)
                    arcTo(circle1.radius, circle1.radius, 0.0, true, false, p11)
                    curveTo(h11, h21, p21)
                    arcTo(circle2.radius, circle2.radius, 0.0, !(spread2 < PI/2), false, p22)
                    curveTo(h22, h12, p12)
                    close()
                })
            }
        }

        val gui = GUI()
        val s = object {
            @DoubleParameter("v", 0.0, 1.0)
            var v: Double = 0.5

            @DoubleParameter("Handle size", 0.0, 5.0)
            var handleSize: Double = 2.6
        }
        gui.add(s, "Metaball settings")

        extend(gui)

        val circle2 = Circle(mouse.position, 55.0)
        val circle3 = Circle(mouse.position, 20.0)
        val metaballPair1 = MetaballPair(Circle(width*0.275, height*0.5, 0.1125*width), circle2, s.v, s.handleSize)
        val metaballPair2 = MetaballPair(Circle(width*0.725, height*0.5, 0.1125*width), circle3, s.v, s.handleSize)

//        val colors = listOf(rgb("#4c5ecf"), rgb("#00fd7c"), rgb(234/255.0,11/255.0,76/255.0).toHSVa().saturate(0.8).shade(1.5).toRGBa())
//        val colors = listOf(rgb("F0C987").shade(1.2), rgb("DB4C40"), rgb("89BD9E"))
//        val colors = listOf(rgb("D3C0D2"), rgb("D5E2BC"), rgb("DAFFEF"))
//        val colors = listOf(rgb("393E41"), rgb("FFFC31"), rgb("E94F37"))
//        val colors = listOf(rgb("F6F7EB"), rgb("FFFC31"), rgb("E94F37").shade(1.3))
//        val colors = listOf(rgb("5C415D"), rgb("FFFC31"), rgb("E94F37"))
        val colors = listOf(ColorRGBa.WHITE, rgb("393E41"), rgb("FFFC31"), rgb("E94F37"))

//        extend(ScreenRecorder()){
//            maximumDuration = 4.0
////            maximumFrames = ((1000 + 2 * 600.0) / (2*4.0)).toLong()
//            profile = GIFProfile()
//            frameRate = 50
//        }

//        extend(TemporalBlur()){
//            fps = 50.0
//            duration = 0.9
////            samples = 5
////            samples = 10
//            linearizeInput = false
//            delinearizeOutput = false
//            jitter = 0.0
//        }

        val c = compose {
            layer {
                val mainCircle = Circle(width/2.0, height/2.0, 120.0)
//                val circles = Vector2.uniforms(20, Vector2.ZERO, Vector2(width*1.0, height*1.0)).map {
//                    Circle(it, gaussian(8.0, 2.0))
//                }
                val circles = List(20) {
                    Circle(mainCircle.center, 10.0)
                }

                val pairs = circles.map {
                    MetaballPair(mainCircle, it)
                }

                val trans = org.openrndr.math.transforms.transform {
                    translate(-mainCircle.center)
                    scale(2.0, 1.0, 1.0)
                    translate((width + 2 * mainCircle.radius)/2, 0.0)
                }
                val ellipse = mainCircle.shape.transform(trans)
//                println(ellipse.bounds.center)
//                println(ellipse.bounds)

//                val speeds = pairs.map { _ -> gaussian(4.0, 1.0)}
                val speed = 8.0
                val speeds = circles.map { _ -> speed }

                draw {


                    val r = 0.06875*width
                    metaballPair1.circle2 = Circle(width*0.275, seconds*width/4.0 % (height+2*r) - r, r)
                    metaballPair2.circle2 = Circle(width*0.725, seconds*width/4.0 % (height+2*r) - r, r/5.5)

                    pairs.forEachIndexed { i, pair ->
                        pair.v = s.v
                        pair.handleSize = s.handleSize
                        pair.v = s.v
                        pair.handleSize = s.handleSize

                        pair.update(seconds)
                        pair.update(seconds)

                        pair.circle2 = pair.circle2.copy(center = pair.circle2.center + Vector2( cos(i.toDouble()/circles.size * 2 * PI), sin(i.toDouble()/circles.size * 2 * PI)) * (if (pair.bonded) 1.0 else 2.0))
                    }

                    drawer.apply {
                        clear(colors[0])
                        fill = colors[1]
                        stroke = ColorRGBa.BLACK
                        stroke = null
                        strokeWeight = 0.0
                        contours(pairs.map { it.contours }.flatten())
//                        fill = colors[2]
//                        contours(pair.contours)
                    }
                }

            }

        }

        extend {
            c.draw(drawer)
        }
    }
}

fun Vector2.Companion.fromRadians(rad: Double, radius: Double = 1.0): Vector2 {
    return Vector2(cos(rad), sin(rad))*radius
}

fun getVector(center: Vector2, angle: Double, radius: Double): Vector2 {
    return Vector2.fromRadians(angle, radius) + center
}




fun metaball(c1: Circle, c2: Circle, v: Double = 0.5, handleSize: Double = 2.5): List<ShapeContour> {
    if (c1.radius <= 0.0){
        if (c2.radius <= 0.0){
            return listOf()
        } else {
            return listOf(c2.contour)
        }
    } else if (c2.radius <= 0.0) {
        return listOf(c1.contour)
    }

    // Make circle1 the larger circle
    val bigger = c1.radius > c2.radius
    val circle1 = if (bigger) c1 else c2
    val circle2 = if (bigger) c2 else c1

    val diff = circle2.center - circle1.center
    val d = diff.length
    if (d <= circle1.radius - circle2.radius){
        return listOf(circle1.contour)
    }

    val totalRadius = circle1.radius + circle2.radius
    val maxSpread1 = acos((circle1.radius - circle2.radius)/d)
    val maxSpread2 = PI - maxSpread1

    val u1 = if (d >= totalRadius) 0.0 else acos((circle1.radius.pow(2) + d.pow(2) - circle2.radius.pow(2)) / (2 * circle1.radius * d))
    val u2 = if (d >= totalRadius) 0.0 else acos((circle2.radius.pow(2) + d.pow(2) - circle1.radius.pow(2)) / (2 * circle2.radius * d))

    val spread1 = u1 + (maxSpread1 - u1) * v
    val spread2 = PI - (u2 + (maxSpread2 - u2) * v)

    val angleBetweenCenters = atan2(diff.y, diff.x)
    val angle11 = angleBetweenCenters + spread1
    val angle12 = angleBetweenCenters - spread1
    val angle21 = angleBetweenCenters + spread2
    val angle22 = angleBetweenCenters - spread2

    val p11 = getVector(circle1.center, angle11, circle1.radius)
    val p12 = getVector(circle1.center, angle12, circle1.radius)
    val p21 = getVector(circle2.center, angle21, circle2.radius)
    val p22 = getVector(circle2.center, angle22, circle2.radius)

    val d2Base = min(v * handleSize, p11.distanceTo(p21) / totalRadius)
    val d2 = d2Base * min(1.0, (d*2) / totalRadius)

    val r1 = circle1.radius * d2
    val r2 = circle2.radius * d2

    val h11 = getVector(p11, angle11 - PI/2, r1)
    val h12 = getVector(p12, angle12 + PI/2, r1)
    val h21 = getVector(p21, angle21 + PI/2, r2)
    val h22 = getVector(p22, angle22 - PI/2, r2)

    val curve1 = contour {
        moveTo(p11)
        curveTo(h11, h21, p21)
    }

    val curve2 = contour {
        moveTo(p22)
        curveTo(h22, h12, p12)
    }

    if (curve1.intersections(curve2).isNotEmpty()){
        return listOf(circle1.contour, circle2.contour)
    }

    return listOf(contour {
        moveTo(p12)
        arcTo(circle1.radius, circle1.radius, 0.0, true, false, p11)
        curveTo(h11, h21, p21)
        arcTo(circle2.radius, circle2.radius, 0.0, !(spread2 < PI/2), false, p22)
        curveTo(h22, h12, p12)
        close()
    })
}