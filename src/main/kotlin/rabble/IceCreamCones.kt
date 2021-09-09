import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsv
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.random
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extras.color.presets.*
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.PI
import org.openrndr.extra.videoprofiles.GIFProfile

fun main() = application {
    configure {
        width = 1000
        height = 1000
    }

    oliveProgram {
        val duration = 1.5
        val RECORD = false

        if (RECORD) {
            extend(ScreenRecorder()) {
                frameRate = 50
                profile = GIFProfile()
            }
        }
        extend(Screenshots())

        val h = 450.0
        val w = 100.0

        class Ball(var pos: Vector2, val r: Double, val color: ColorRGBa): Animatable() {
            val contour get() = Circle(pos, r).contour
        }

        class Cone(var pos: Vector2, val balls: List<Ball>, var rot: Double = 0.0): Animatable() {
            val cone = contour {
                moveTo(0.0, h)
                lineTo(-w, 0.0)
                lineTo(w, 0.0)
                close()
            }

            fun draw(drawer: Drawer){
                drawer.isolated {
                    translate(pos)
                    rotate(rot)

                    balls.forEachIndexed { i, ball ->
                        fill = ball.color
                        contour(ball.contour)
                    }

                    fill = ColorRGBa.WHITE
                    contour(cone)
                }
            }

            fun update(){
                balls.forEach {
                    it.updateAnimation()
                }
            }
        }

//        var balls = List(5) {
//            Ball(Circle(Vector2.gaussian(Vector2.ZERO, Vector2(25.0, 45.0)), 50.0).contour, hsv(random(0.0, 360.0), 0.6, 1.0).toRGBa())
//        }

//        mouse.buttonUp.listen {
//            balls = List(5) {
//                Ball(Circle(Vector2.uniform(Vector2(0.0, -60.0), Vector2(25.0, 15.0)), 50.0).contour, hsv(random(0.0, 360.0), 0.6, 1.0).toRGBa())
//            }
//        }

        fun b(x: Double, y: Double, r: Double, color: ColorHSVa) = Ball(Vector2(x, y), r, color.toRGBa())

        val balls = listOf(
            b(-40.0, -20.0, 60.0, hsv(50.0, 0.6, 1.0)),
            b(-20.0, -70.0, 60.0, hsv(100.0, 0.6, 1.0)),
            b(10.0, -0.0, 80.0, hsv(0.0, 0.6, 1.0)),
        )

        val cones = LineSegment(Vector2(0.0, height*1.0), Vector2(width*1.0, 0.0)).contour.equidistantPositions(5).map { Cone(it, balls, -10.0) }
//            Cone(Vector2(width/2.0, height/2.0), balls, -10.0)
//        }

        balls[0].apply{
            ::pos.animate(Vector2(-100.0, 100.0), 1000)
        }

        extend {
            if (seconds >= duration && RECORD){
                application.exit()
            }

            for (cone in cones){
                cone.update()
                cone.draw(drawer)
            }
        }
    }
}