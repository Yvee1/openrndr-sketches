import org.openrndr.animatable.Animatable
import org.openrndr.application
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
import org.openrndr.extras.color.spaces.ColorHSLUVa
import org.openrndr.extras.color.spaces.ColorXSLUVa
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Matrix44
import org.openrndr.math.Polar.Companion.fromVector
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.shape.*
import org.openrndr.svg.loadSVG
import kotlin.math.*

fun main() = application {
    configure {
        width = 2000
        height = 1000
    }

    program {
        val POINTS = false
        val RECORD = false
        val GIF = true

        val duration = 3.0

        val tilingSettings = object {
            @IntParameter("a", 1, 15)
            var a: Int = 6

            @IntParameter("b", 1, 15)
            var b: Int = 5
        }

        val colorSettings = object {
            @DoubleParameter("Scaling", 1.0, 20.0)
            var s: Double = 6.78

            @DoubleParameter("Hue translation", 0.0, 360.0)
            var t: Double = 240.0

            @DoubleParameter("Saturation", 0.0, 1.0)
            var sat: Double = 0.94

            @DoubleParameter("Lightness", 0.0, 1.0)
            var light: Double = 0.9
        }

        val res = 1
        val minRes = 4.0
        val maxRes = 10.0

        data class Shape(var pts: List<Vector2>, val closed: Boolean, val strokeColor: ColorRGBa, val fillColor: ColorRGBa = ColorRGBa.BLACK, val strokeWeight: Double = 0.1)

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
                drawer.strokeWeight = shape.strokeWeight
                drawer.circles(pts, 2.0)
            } else {
                drawer.fill = shape.fillColor
                drawer.stroke = shape.strokeColor
                drawer.strokeWeight = shape.strokeWeight
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
                drawer.strokeWeight = shape.strokeWeight
                drawer.circles(pts, 2.0)
            } else {
                drawer.fill = shape.fillColor
                drawer.stroke = shape.strokeColor
                drawer.strokeWeight = shape.strokeWeight
                drawer.contour(ShapeContour.fromPoints(pts, shape.closed))
            }
        }

        fun drawTransformed(drawer: Drawer, ls: LineSegment, strokeColor: ColorRGBa){
            drawTransformed(drawer, Shape(ls.contour.equidistantPositions(res), false, strokeColor))
        }

        val thorns = loadSVG("data/images/thorns.svg")
        var shapes: List<Shape> = listOf()

        val extra = 5
        val n = 5
        val w =  2 * PI / n
        val gap = 0.15

        val sym1 = Vector2( w, 0.0)
        val sym2 = Vector2(0.0,  w)

        val a = tilingSettings.a
        val b = tilingSettings.b
        val c = sym1 * a.toDouble() + sym2 * b.toDouble()
        val rotAngle = 90-fromVector(c).theta
        val scaling = 2 * PI / c.rotate(rotAngle).length

        var grid = List((n + 2*extra) * (n + 2*extra)) {
            val i = (it % (n + 2*extra)) - extra
            val j = (it / (n + 2*extra)) - extra
            val x = i.toDouble() / (n) * 2.0 * PI
            val y = j.toDouble() / (n) * 2.0 * PI
            val c = contour {
                moveTo(Vector2(x, y))
                lineTo(Vector2(x + w*(1-gap), y))
                lineTo(Vector2(x + w*(1-gap), y + w*(1-gap)))
                lineTo(Vector2(x, y + w*(1-gap)))
                close()
            }
//            Shape(c.equidistantPositions(res), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
//            println(clamp(res * exp(x)/10.0, 10.0, 1000.0).toInt())
            val newX = (Vector2(x, y).rotate(rotAngle, Vector2(PI, PI)) * scaling).x
            Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), c.closed, ColorRGBa.BLACK, ColorRGBa.PINK)
//            Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
        }

        var vines1 = List((n + 2*extra) * (n + 2*extra)) {
            val i = (it % (n + 2*extra)) - extra
            val j = (it / (n + 2*extra)) - extra
            val x = i.toDouble() / (n) * 2.0 * PI
            val y = j.toDouble() / (n) * 2.0 * PI
            val c = contour {
                moveTo(Vector2(x - 0.1*w, y- 0.1*w))
                curveTo(Vector2(x + w*(1-gap)*0.5, y - 0.2*w), Vector2(x + w*(1-gap) + 0.1*w, y- 0.1*w))
                close()
            }
//            Shape(c.equidistantPositions(res), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
//            println(clamp(res * exp(x)/10.0, 10.0, 1000.0).toInt())
            val newX = (Vector2(x, y).rotate(rotAngle, Vector2(PI, PI)) * scaling).x
            Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), c.closed, ColorRGBa.BLACK, ColorRGBa.GREEN, 0.0)
//            Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
        }

        var vines2 = List((n + 2*extra) * (n + 2*extra)) {
            val i = (it % (n + 2*extra)) - extra
            val j = (it / (n + 2*extra)) - extra
            val x = i.toDouble() / (n) * 2.0 * PI
            val y = j.toDouble() / (n) * 2.0 * PI
            val c = contour {
                moveTo(Vector2(x - 0.1*w, y- 0.1*w))
                curveTo(Vector2(x - 0.2*w, y + w*(1-gap)*0.5), Vector2(x - 0.1*w, y  + w*(1-gap) + 0.1*w))
                close()
            }
//            Shape(c.equidistantPositions(res), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
//            println(clamp(res * exp(x)/10.0, 10.0, 1000.0).toInt())
            val newX = (Vector2(x, y).rotate(rotAngle, Vector2(PI, PI)) * scaling).x
            Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), c.closed, ColorRGBa.BLACK, ColorRGBa.GREEN, 0.0)
//            Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
        }

        println(thorns.findShapes()[0].effectiveShape.transform(Matrix44.scale(0.001, 0.001, 1.0)).contours[0].equidistantPositions(100))
        val thornShape = Shape(thorns.findShapes()[0].effectiveShape.transform(Matrix44.scale(0.0001, 0.0001, 1.0) * Matrix44.translate(-1.2, -1.2, 0.0)).contours[0].equidistantPositions(1000), true, ColorRGBa.BLACK)
        shapes = grid

        val gui = GUI()
        gui.add(tilingSettings, "Tiling settings")
        gui.add(colorSettings, "Color settings")
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

        gui.onChange { _, _ ->
            grid = List((n + 2*extra) * (n + 2*extra)) {
                val i = (it % (n + 2*extra)) - extra
                val j = (it / (n + 2*extra)) - extra
                val x = i.toDouble() / (n) * 2.0 * PI
                val y = j.toDouble() / (n) * 2.0 * PI
                val c = contour {
                    moveTo(Vector2(x, y))
                    lineTo(Vector2(x + w*(1-gap), y))
                    lineTo(Vector2(x + w*(1-gap), y + w*(1-gap)))
                    lineTo(Vector2(x, y + w*(1-gap)))
                    close()
                }

//            Shape(c.equidistantPositions(res), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
//            println(clamp(res * exp(x)/10.0, 10.0, 1000.0).toInt())
                val newX = (Vector2(x, y).rotate(rotAngle, Vector2(PI, PI)) * scaling).x
                Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), true, ColorRGBa.BLACK, ColorRGBa.PINK)
//            Shape(c.equidistantPositions(4 * clamp(res * exp(newX)/10.0, minRes, maxRes).toInt()), true, ColorRGBa.BLACK, ColorRGBa(i.toDouble() / (n+extra), j.toDouble() / (n+extra), 1.0, 1.0))
            }


            shapes = grid
        }


//        print(thorns.findShapes()[0].effectiveShape.transform(Matrix44.scale(0.1, 0.1, 1.0)).contours.size)

        extend {
            if (seconds >= duration && RECORD){
                application.exit()
            }

            val a = tilingSettings.a
            val b = tilingSettings.b
            val c = sym1 * a.toDouble() + sym2 * b.toDouble()
            val rotAngle = 90-fromVector(c).theta
            val scaling = 2 * PI / c.rotate(rotAngle).length
//            val newGrid = grid.map { shape -> Shape(shape.pts.map { pt -> (pt + sym1 * ((seconds / duration) % 1)).rotate(rotAngle, Vector2(PI, PI)) * scaling }, shape.closed, shape.strokeColor, shape.fillColor)}
            val newShapes = shapes.map { shape ->
                val newPts = shape.pts.map { pt -> (pt + sym1 * ((seconds / duration) % 1)).rotate(rotAngle, Vector2(PI, PI)) * scaling }
                Shape(newPts, shape.closed, shape.strokeColor, ColorXSLUVa((newPts[0].x.pow(2) * colorSettings.s) + colorSettings.t, colorSettings.sat, colorSettings.light, 1.0).toRGBa(), shape.strokeWeight)
            }

//            drawer.composition(thorns)
//            drawer.contour(thorns.findShapes()[0].effectiveShape.transform(Matrix44.scale(0.1, 0.1, 1.0)).contours[0])

            drawer.isolated {
                drawer.clear(ColorRGBa.WHITE)
//                strokeWeight = 0.1
                drawer.drawStyle.clip = Rectangle(0.0, 0.0,height*1.0, height*1.0)
                newShapes.forEach { drawInSquareGrid(drawer, it) }

                translate(3*height/2.0, height/2.0)
                drawer.drawStyle.clip = Rectangle(height*1.0, 0.0,height*1.0, height*1.0)
                newShapes.forEach { drawTransformed(drawer, it) }
            }


        }
    }
}

//fun toRadians(degrees: Double): Double {
//    return degrees / 180.0 * PI
//}