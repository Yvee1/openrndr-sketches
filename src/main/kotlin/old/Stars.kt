import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineJoin
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.shapes.regularStar
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.shapes.regularStarBeveled
import org.openrndr.extra.shapes.regularStarRounded
import org.openrndr.extras.color.presets.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import org.openrndr.svg.loadSVG
import kotlin.math.PI
import kotlin.math.max
import kotlin.random.Random

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        val butterfly = loadSVG("data/images/butterfly.svg")

        val s = object {
            @ColorParameter("Background color", order=-1)
            var bgColor = ColorRGBa.DARK_RED

            @IntParameter("Points", 1, 10, 0)
            var points = 5

            @DoubleParameter("Inner radius", 0.0, 15.0, order=1)
            var innerRadius = 10.0

            @DoubleParameter("Outer radius", 0.0, 15.0, order=2)
            var outerRadius = 20.0

            @DoubleParameter("Inner factor", 0.0, 2.0, order=3)
            var innerFactor = 0.5

            @DoubleParameter("Outer factor", 0.0, 2.0, order=4)
            var outerFactor = 1.0
        }

        val bloom = GaussianBloom()

        val gui = GUI()
        gui.add(s, "Settings")
        bloom.addTo(gui)
        bloom.sigma = 1.0
        bloom.window = 1
        bloom.gain = 4.0
        bloom.shape = 1.0

        val shape = butterfly.findShapes()[0].effectiveShape.transform(Matrix44.scale(0.6, 0.6, 1.0)*Matrix44.translate(20.0, 200.0, 0.0))
//        var points = poissonDiskSampling(width*1.1, height*1.1, 60.0, 400, true) { _, _, v ->
//            shape.closedContours.any { it.contains(v) }
//        }
        fun packCircles(shape: Shape, r: Double): List<Circle> {
            val rect = shape.bounds
            val points = poissonDiskSampling(rect.width, rect.height, r, 200) { _, _, v ->
                shape.closedContours.any { (v + rect.corner) in it }
            }.map { it+rect.corner }

            return points.map { Circle(it, r/2) }.filter { circle ->
                circle.contour.equidistantPositions(50).all { p -> shape.closedContours.any { p in it } }
            }
        }
        val circles = packCircles(shape, 40.0)
        var points = circles.map { it.center }
//        var points = drawer.bounds.shape.randomPoints(4000)
//        println(shape.contains(Vector2(200.0, 200.0)))
//        points = points.filter { shape.contains(it) }
//        println(points)
//        var stars = points.map { regularStarRounded(s.points, s.innerRadius, s.outerRadius, s.innerFactor, s.outerFactor, it, Double.uniform(0.0, 360.0)) }
        var stars = points.map { FiveStar(it, 10.0, 5.0) }

        gui.onChange { _, _ ->
            stars = points.map { FiveStar(it, 10.0, 5.0) }
//            stars = points.map { regularStarRounded(s.points, s.innerRadius, s.outerRadius, s.innerFactor, s.outerFactor, it, Double.uniform(0.0, 360.0)) }
//            stars = points.map { regularStar(s.points, s.innerRadius, s.outerRadius, it, Double.uniform(0.0, 360.0)) }
        }

        val c = compose {
            layer {
                draw {
                    drawer.apply {
                        clear(s.bgColor)
                    }
                }
            }

            layer {
//                mask {
//                    drawer.apply {
//                        composition(butterfly)
//                    }
//                }

                draw {
                    drawer.apply {
                        stars.forEach { it.draw(this) }
//                        this.points(points)
//                        contour(regularStarRounded(4, 10.0, 100.0, 0.0, 0.0, drawer.bounds.center))
//                        circles(points, 5.0)
                        this.circles(circles)
                        stroke = ColorRGBa.BLACK
                        fill = null
//                        this.shape(shape)
//                        circles(shape.randomPoints(100), 10.0)
                    }

                }
//            }
                post(bloom)
            }
        }

        extend(gui)
        extend {
            c.draw(drawer)
            drawer.apply {
                fill = null
                this.shape(shape)

            }
//            drawer.composition(butterfly)
        }
    }
}

fun Boolean.Companion.coinToss(random: Random = Random.Default): Boolean {
    return Boolean.bernoulli(0.5, random)
}

fun Boolean.Companion.bernoulli(p: Double, random: Random = Random.Default): Boolean {
    return Double.uniform(0.0, 1.0, random) < p
}

class FiveStar {
    var shapeContour: ShapeContour
    var rot: Double
    var color: ColorRGBa
    var useFill: Boolean

    var outerR: Double
    var innerR: Double
    var innerF: Double
    var outerF: Double
    var pos: Vector2

    constructor(pos: Vector2, meanR: Double, devR: Double, color: ColorRGBa = ColorRGBa.GOLD) {
        outerR = max(gaussian(meanR, devR), 3.0)
        innerR = Double.uniform(max(1.0, outerR*0.2), outerR*0.5)
        innerF = Double.uniform(0.0, 0.8)
        outerF = Double.uniform(0.0, 0.8)
        this.pos = pos

        rot = Double.uniform(0.0, 360.0)
        shapeContour = regularStarRounded(5, innerR, outerR, innerF, outerF, pos, rot)
        useFill = Boolean.coinToss()
        this.color = color
    }

    constructor(pos: Vector2, outerR: Double, innerR: Double, outerF: Double, innerF: Double, rot: Double, color: ColorRGBa, useFill: Boolean){
        this.pos = pos
        this.outerR = outerR
        this.innerR = innerR
        this.outerF = outerF
        this.innerF = innerF
        this.rot = rot
        this.color = color
        this.useFill = useFill
        shapeContour = regularStarRounded(5, innerR, outerR, innerF, outerF, pos, rot)
    }

    fun draw(drawer: Drawer){
        this.draw(drawer, this.color)
    }

    fun draw(drawer: Drawer, fillOrStrokeColor: ColorRGBa){
        drawer.apply {
            if (useFill) {
                fill = fillOrStrokeColor
                stroke = null
            } else {
                fill = null
                stroke = fillOrStrokeColor
                lineJoin = LineJoin.MITER
            }
            contour(shapeContour)
        }
    }

    fun scale(sc: Double): FiveStar {
        return FiveStar(pos, outerR*sc, innerR*sc, outerF, innerF, rot, color, useFill)
    }
}