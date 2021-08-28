import org.openrndr.animatable.Animatable
import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSLa
import org.openrndr.color.ColorXSVa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.*
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Polar.Companion.fromVector
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.*

fun main() = applicationSynchronous {
    configure {
        width = 1000
        height = 1000
    }

    oliveProgram {
        val POINTS = false
        val RECORD = true
        val GIF = true

        val duration = 3.0

        val tilingSettings = object {
            @IntParameter("a", 1, 15)
            var a: Int = 2

            @IntParameter("b", 1, 15)
            var b: Int = 1
        }

        val s = object : Animatable() {
            @IntParameter("Stripes", 0, 20)
            var stripes: Int = 2

            @DoubleParameter("Radius", 1.0, 500.0)
            var r: Double = 117.5

            @IntParameter("x-repeat", 0, 20)
            var xRepeat: Int = 10

            @IntParameter("y-repeat", 0, 20)
            var yRepeat: Int = 3

            @ColorParameter("Background")
            var bg: ColorRGBa = ColorRGBa.BLACK

            @DoubleParameter("Offset", -0.1, 0.2)
            var offset: Double = 0.0

            @DoubleParameter("Rotated cube", 0.0, 120.0)
            var rotated: Double = 0.0

            @DoubleParameter("Rotated tiling", -180.0, 180.0)
            var rotatedTiling: Double = 0.0
        }


        val res = 20
        val minRes = 4.0
        val maxRes = 10.0

        data class Shape(var pts: List<Vector2>, val closed: Boolean, val strokeColor: ColorRGBa, val fillColor: ColorRGBa = ColorRGBa.BLACK)

        fun complexExp(pt: Vector2): Vector2 {
            return Vector2(cos(pt.y), sin(pt.y)) * exp(pt.x)
        }

        fun shrink(pt: Vector2): Vector2 {
            return Vector2(pt.x / height * 2 * PI, pt.y / height * 2 * PI)
        }

        fun grow(pt: Vector2): Vector2 {
            return Vector2(pt.x / (2 * PI) * height, pt.y / (2 * PI) * height)
        }

        fun drawInSquareGrid(drawer: Drawer, ls: LineSegment){
            drawer.lineSegment(grow(ls.start), grow(ls.end))
        }

        fun drawInSquareGrid(drawer: Drawer, shape: Shape){
            val pts = shape.pts.map { grow(it) }
            if (POINTS){
                drawer.fill = null
                drawer.stroke = shape.strokeColor
                drawer.circles(pts, 2.0)
            } else {
                drawer.fill = shape.fillColor
                drawer.stroke = shape.strokeColor
                drawer.contour(ShapeContour.fromPoints(pts, shape.closed))
            }
        }

        fun transform(pts: List<Vector2>): List<Vector2> {
            return pts.map { grow(complexExp(it)) * 0.005 }
        }

        fun transform(ls: LineSegment): List<Vector2> {
            val pts = ls.contour.equidistantPositions(res)
            return transform(pts)
        }

        fun drawTransformed(drawer: Drawer, shape: Shape){
            val pts = transform(shape.pts)
            if (POINTS){
                drawer.fill = null
                drawer.stroke = shape.strokeColor
                drawer.circles(pts, 2.0)
            } else {
                drawer.fill = shape.fillColor
                drawer.stroke = shape.strokeColor
                drawer.contour(ShapeContour.fromPoints(pts, shape.closed))
            }
        }

        fun drawTransformed(drawer: Drawer, ls: LineSegment, strokeColor: ColorRGBa){
            drawTransformed(drawer, Shape(ls.contour.equidistantPositions(res), false, strokeColor))
        }

//        val extra = 4
//        val nn = 5
//        val w =  2 * PI / nn

//        val sym1 = Vector2(w, 0.0)
//        val sym2 = Vector2(0.0, w)
//
//        val a = tilingSettings.a
//        val b = tilingSettings.b
//        val c = sym1 * a.toDouble() + sym2 * b.toDouble()
//        val rotAngle = 90-fromVector(c).theta
//        val scaling = 2 * PI / c.rotate(rotAngle).length
//
//        val grid = List((nn + 2*extra) * (nn + 2*extra)) {
//            val i = (it % (nn + 2*extra)) - extra
//            val j = (it / (nn + 2*extra)) - extra
//            val x = i.toDouble() / (nn) * 2.0 * PI
//            val y = j.toDouble() / (nn) * 2.0 * PI
//            val c = contour {
//                moveTo(Vector2(x, y))
//                lineTo(Vector2(x + w, y))
//                lineTo(Vector2(x + w, y + w))
//                lineTo(Vector2(x, y + w))
//                close()
//            }
////            Shape(c.equidistantPositions(res), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
////            println(clamp(res * exp(x)/10.0, 10.0, 1000.0).toInt())
//            val newX = (Vector2(x, y).rotate(rotAngle, Vector2(PI, PI)) * scaling).x
//            Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), true, ColorRGBa.BLACK, ColorRGBa.PINK)
////            Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
//        }


        /////////////////////

        /////////////////////
        val gui = GUI()
        gui.add(tilingSettings)
        gui.add(s, "Hexacube settings")
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
        } else {
            extend(gui)
        }
        extend {
            if (seconds >= duration && RECORD){
                application.exit()
            }

            drawer.apply {
                clear(ColorRGBa.WHITE)
                stroke = ColorRGBa.BLACK
                fill = ColorRGBa.BLACK
            }

            val pos = shrink(mouse.position)
            val l1 = LineSegment(pos.x, pos.y, pos.x, 0.0)
            val l2 = LineSegment(0.0, pos.y, pos.x, pos.y)
            val l3 = LineSegment(Vector2.ZERO, pos)

//            val c = contour {
//                moveTo(Vector2(1.0, 1.0))
//                lineTo(Vector2(5.0, 1.0))
//                lineTo(Vector2(5.0, 5.0))
//                lineTo(Vector2(1.0, 5.0))
//            }
//            val s = Shape(c.equidistantPositions(res), true, ColorRGBa.BLACK, ColorRGBa.PINK)

//            val a = tilingSettings.a
//            val b = tilingSettings.b
//            val c = sym1 * a.toDouble() + sym2 * b.toDouble()
//            val rotAngle = 90-fromVector(c).theta
//            val scaling = 2 * PI / c.rotate(rotAngle).length
//            val newGrid = grid.map { shape -> Shape(shape.pts.map { pt -> (pt + sym1 * ((seconds / duration) % 1)).rotate(rotAngle, Vector2(PI, PI)) * scaling }, shape.closed, shape.strokeColor, shape.fillColor)}

//            drawer.isolated {
//                strokeWeight = 0.1
//                drawer.drawStyle.clip = Rectangle(0.0, 0.0,height*1.0, height*1.0)
//                newGrid.forEach { drawInSquareGrid(drawer, it) }
//
//                translate(3*height/2.0, height/2.0)
//                drawer.drawStyle.clip = Rectangle(height*1.0, 0.0,height*1.0, height*1.0)
//                newGrid.forEach { drawTransformed(drawer, it) }
//            }

            val n = 6
            val r = s.r
            val stripes = s.stripes

            val sym1 = shrink(Vector2(cos(PI / 6) * r * 6, 0.0))
            val sym2 = shrink(Vector2(0.0, 2 * cos(PI / 6) * r + 2 * r))
            val a = tilingSettings.a
            val b = tilingSettings.b
            val c = sym1 * a.toDouble() + sym2 * b.toDouble()
//            val rotAngle = 90-fromVector(c).theta
            val rotAngle = 0.0
            val scaling = 2 * PI / c.rotate(rotAngle).length

//            val scaling = 1.0

            val shapes: MutableList<Shape> = mutableListOf()

            val color = arrayOf(
                ColorRGBa.fromHex("#FAA613"),
                ColorRGBa.fromHex("#688E26"),
                ColorRGBa.fromHex("#F44708"),
            )

            val points = List(n) { i ->
                val theta = 2*PI/n * i + PI/n
                Vector2(r * cos(theta), r * sin(theta))
            }

            fun outer(i: Int): LineSegment {
                // precondition: i is in [0, n-1)
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
                return this.contour.movingPositions(stripes*2, (seconds+1.5) * 4 / duration % 1, reversed=reversed, offset = if (s.offset > 0.09) 0.3 else s.offset)
            }

            fun getSide(innerSide: List<Pair<Double, Vector2>>, outerSide: List<Pair<Double, Vector2>>, j: Int, k: Int): List<ShapeContour> {
                val l: MutableList<ShapeContour> = mutableListOf()
                val trans = Vector2(width/2.0 + (j + (k % 2) * 0.5) * cos(PI / 6) * r * 2, height / 2.0 +k * 1.5 * r)
                for (i in 0 until stripes + 1) {
                    if (outerSide[2 * i + 1].first > 0 && outerSide[2*i].first < 1) {
                        val pts = listOf(
                            shrink(innerSide[2 * i].second + trans),
                            shrink(outerSide[2 * i].second + trans),
                            shrink(outerSide[2 * i + 1].second + trans),
                            shrink(innerSide[2 * i + 1].second + trans),
                            shrink(innerSide[2 * i].second + trans)
                        )
                        l.add(ShapeContour.fromPoints(pts, true))
                    }
                }
                return l.toList()
            }

            fun drawSide(drawer: Drawer, side: List<ShapeContour>){
                side.forEach {
                    drawer.contour(it)
                }
            }

            fun addSide(shapes: MutableList<Shape>, side: List<ShapeContour>, colorIndex: Int){
                shapes.addAll(side.map { Shape(it.equidistantPositions(4 * res), it.closed, color[colorIndex], color[colorIndex]) })
            }

            for (k in -s.yRepeat - 1 until s.yRepeat) {
                if (k == 1 || k == -3){
                    continue
                }

                for (j in -s.xRepeat until s.xRepeat + 1) {
                    val kind = Math.floorMod(j + 2 * (k % 2), 3)

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
                            val side1 = getSide(i0ps, o2ps, j, k)
                            addSide(shapes, side1, 0)

                            val side2 = getSide(i1ps, o4ps, j, k)
                            addSide(shapes, side2, 1)

                            val side3 = getSide(i0ps_, o5ps, j, k)
                            addSide(shapes, side3, 1)
                        }

                        1 -> {
                            val side1 = getSide(i0ps, o2ps, j, k)
                            addSide(shapes, side1, 0)

                            val side2 = getSide(i2ps_, o3ps, j, k)
                            addSide(shapes, side2, 0)

                            val side3 = getSide(i2ps, o0ps, j, k)
                            addSide(shapes, side3, 2)
//                                drawSide(this, side3)
                        }

                        2 -> {

                            val side1 = getSide(i1ps_, o1ps, j, k)
                            addSide(shapes, side1, 2)
//
                            val side2 = getSide(i1ps, o4ps, j, k)
                            addSide(shapes, side2, 1)

                            val side3 = getSide(i2ps, o0ps, j, k)
                            addSide(shapes, side3, 2)
                        }

                        else -> println("Unknown kind")
                    }
                }
            }

            val newShapes = shapes.map { shape -> Shape(shape.pts.map { pt -> (pt + sym1 * ((seconds / duration) % 1)).rotate(rotAngle, Vector2(PI, PI)) * scaling }, shape.closed, shape.strokeColor, shape.fillColor)}

            drawer.isolated {
                drawer.clear(s.bg)
                strokeWeight = 0.1
//                drawer.drawStyle.clip = Rectangle(0.0, 0.0,height*1.0, height*1.0)
//                newShapes.forEach { drawInSquareGrid(drawer, it) }
//
                translate(1*height/2.0, height/2.0)
//                drawer.drawStyle.clip = Rectangle(height*1.0, 0.0,height*1.0, height*1.0)
                newShapes.forEach { drawTransformed(drawer, it) }
            }
        }
    }
}

//fun toRadians(degrees: Double): Double {
//    return degrees / 180.0 * PI
//}