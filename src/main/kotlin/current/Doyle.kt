package current

import inverse_kinematics.toDegrees
import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.tint
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.mask
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.random
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import useful.FPSDisplay
import kotlin.math.*

// see https://bl.ocks.org/robinhouston/6096562

fun main() = applicationSynchronous {
    configure {
        width = 1000
        height = 1000
        windowResizable = true
    }

    program {
        fun _d(z: Double, t: Double, p: Int, q: Int): Double {
            val w = z.pow(p.toDouble()/q)
            val s = (p*t + 2*PI)/q
            return (z*cos(t) - w*cos(s)).pow(2) + (z*sin(t) - w*sin(s)).pow(2)
        }

        fun ddz_d(z: Double, t: Double, p: Int, q: Int): Double {
            val w = z.pow(p.toDouble()/q)
            val s = (p*t + 2*PI)/q
            val ddz_w = (p.toDouble()/q)*z.pow((p-q)/q)
            return (2*(w*cos(s) - z*cos(t))*(ddz_w*cos(s) - cos(t))
                    + 2*(w*sin(s) - z*sin(t))*(ddz_w*sin(s) - sin(t)))
        }

        fun ddt_d(z: Double, t: Double, p: Int, q: Int): Double {
            val w = z.pow(p.toDouble()/q)
            val s = (p*t + 2*PI)/q
            val dds_t = p.toDouble()/q
            return ( 2*( z*cos(t) - w*cos(s) )*( -z*sin(t) + w*sin(s)*dds_t )
                    + 2*( z*sin(t) - w*sin(s) )*(  z*cos(t) - w*cos(s)*dds_t ))
        }

        fun _s(z: Double, t: Double, p: Int, q: Int): Double {
            return (z + z.pow(p.toDouble()/q)).pow(2)
        }

        fun ddz_s(z: Double, t: Double, p: Int, q: Int): Double {
            val w = z.pow(p.toDouble()/q)
            val ddz_w = (p.toDouble()/q)*z.pow((p-q)/q)
            return 2*(w+z)*(ddz_w+1)
        }

        fun _r(z: Double, t: Double, p: Int, q: Int): Double {
            return _d(z,t,p,q) / _s(z,t,p,q)
        }

        fun ddz_r(z: Double, t: Double, p: Int, q: Int): Double {
            return (ddz_d(z,t,p,q) * _s(z,t,p,q) - _d(z,t,p,q) * ddz_s(z,t,p,q)) / _s(z,t,p,q).pow(2)
        }

        fun ddt_r(z: Double, t: Double, p: Int, q: Int): Double {
            return (ddt_d(z,t,p,q) * _s(z,t,p,q)) / _s(z,t,p,q).pow(2)
        }

        data class DoyleResult(val a: Vector2, val b: Vector2, val r: Double, val mod_a: Double, val arg_a: Double)

        var epsilon = 1e-10
        fun doyle(p: Int, q: Int, zInit: Double, tInit: Double): DoyleResult {
            fun _f(z: Double, t: Double): Double {
                return _r(z,t,0,1) - _r(z,t,p,q);
            }

            fun ddz_f(z: Double, t: Double): Double {
                return ddz_r(z,t,0,1) - ddz_r(z,t,p,q);
            }

            fun ddt_f(z: Double, t: Double): Double {
                return ddt_r(z,t,0,1) - ddt_r(z,t,p,q);
            }

            fun _g(z: Double, t: Double): Double {
                return _r(z,t,0,1) - _r(z.pow( p.toDouble()/q), (p*t + 2*PI)/q, 0,1);
            }

            fun ddz_g(z: Double, t: Double): Double {
                return ddz_r(z,t,0,1) - ddz_r(z.pow(p.toDouble()/q), (p*t + 2*PI)/q, 0,1) * (p.toDouble()/q)*z.pow((p-q)/q);
            }

            fun ddt_g(z: Double, t: Double): Double{
                return ddt_r(z,t,0,1) - ddt_r(z.pow(p.toDouble()/q), (p*t + 2*PI)/q, 0,1) * (p.toDouble()/q);
            }

            data class FindRootResult(val z: Double, val t: Double, val r: Double)

            fun find_root(zInit: Double, tInit: Double): FindRootResult? {
                var z = zInit
                var t = tInit

                for(i in 0 until 100){
                    val v_f = _f(z, t)
                    val v_g = _g(z, t)

//                    println(v_f)
//                    println(v_g)

                    if(-epsilon < v_f && v_f < epsilon && -epsilon < v_g && v_g < epsilon)
                        return FindRootResult(z, t, sqrt(_r(z, t, 0, 1)))

                    val a = ddz_f(z, t)
                    val b = ddt_f(z, t)
                    val c = ddz_g(z, t)
                    val d = ddt_g(z, t)
                    val det = a*d-b*c

                    if (-epsilon < det && det < epsilon)
                        return null

                    z -= (d*v_f - b*v_g)/det;
                    t -= (a*v_g - c*v_f)/det;

                    if (z < epsilon)
                        return null
                }
                return null
            }

            val root = find_root(3.0, 1.0) ?: find_root(2.0, 0.0) ?: find_root(0.5, 0.5) ?: find_root(1.0, 0.0)!!
            val a = Vector2(root.z * cos(root.t), root.z * sin(root.t))
            val zCoRoot = root.z.pow(p.toDouble()/q)
            val tCoRoot = (p*root.t + 2*PI)/q
            val b = Vector2(zCoRoot * cos(tCoRoot), zCoRoot * sin(tCoRoot))
            return DoyleResult(a, b, root.r, root.z, root.t)
        }

        fun Vector2.cmul(other: Vector2): Vector2 {
            return Vector2(x * other.x - y * other.y, x * other.y + y * other.x)
        }

        val settings = object {
            @IntParameter("p", 1, 50)
            var p = 1

            @IntParameter("q", 1, 50)
            var q = 3

            @DoubleParameter("Loop duration", 0.5, 10.0)
            var loopDuration = 3.0

            @BooleanParameter("Mask")
            var enableMask = true
        }
        val gui = GUI()
        gui.add(settings, "Doyle spiral parameters")

//        println(doyle(settings.p, settings.q, 3.0, 1.0))
//        println(doyle(1, 3, 3.0, 1.0))

        fun Circle.scaledFrom(scaleCenter: Vector2, amount: Double): Circle =
           Circle(amount * center.x, amount * center.y, amount * radius)

        val comp = compose {
            draw {
                drawer.apply {
                    clear(ColorRGBa.WHITE)
                    strokeWeight = 5.0
                    circle(Circle(Vector2(width/2.0, height/2.0), min(width, height).toDouble()/3 + 2.5))
                }
            }

            layer {

                mask {
                    val mainCircle = Circle(Vector2(width / 2.0, height / 2.0), min(width, height).toDouble() / 3)
                    if (settings.enableMask) {
                        invertMask = false
                        drawer.circle(mainCircle)
                    } else {
                        invertMask = true
                    }
                }


                draw {
                    val root = doyle(settings.p, settings.q, 3.0, 1.0)
                    val mod_b = hypot(root.b[0], root.b[1])
                    val t = (seconds * settings.p / settings.loopDuration) % settings.p

                    drawer.apply {
//                        strokeWeight = 10.0
                        clear(ColorRGBa.WHITE)
                        translate(width / 2.0, height / 2.0)
//                scale(5.0, 5.0)
                        val scaleAmount = root.mod_a.pow(t-10)
//                        val scaleAmount = 1.0
//                        scale(scaleAmount)
                        rotate((root.arg_a * t).toDegrees())
//                circle(root.a[0], root.a[1], sqrt(root.r * root.mod_a)/2)
//                circle(root.b[0], root.b[1], sqrt(root.r * mod_b)/2)
                        // Nahh
                        val max_d = width * 5 / scaleAmount
                        var q = Vector2(0.01, 0.0)

                        for (t in 0 until settings.p) {
                            stroke = ColorRGBa.BLACK
                            fill = if (t == 0) ColorRGBa.BLACK else null
//                            fill = ColorHSVa((t.toDouble()/settings.p)*360.0 % 360, 0.8, 0.8).toRGBa()

                            var c = q.copy()
                            var d = hypot(c.x, c.y)
                            while (d < max_d) {
//                                circle(Circle(c, d * root.r*1.01).scaledBy(scaleAmount))
//                                circle(Circle(c, d * root.r*settings.extra+3).scaledFrom(Vector2.ZERO, scaleAmount))
                                val theCircle = Circle(c, d * root.r).scaledFrom(Vector2.ZERO, scaleAmount)
                                strokeWeight = sqrt(theCircle.radius)
                                circle(theCircle.copy(radius = theCircle.radius + 1))
                                d *= mod_b
                                c = c.cmul(root.b)
                            }
                            q = q.cmul(root.a)
                        }
                    }
                }
            }
        }


        val font = loadFont("data/fonts/default.otf", 18.0)
        val RECORD = false

//

        if(RECORD) {
            extend(ScreenRecorder()) {
                profile = GIFProfile()
                frameRate = 50
            }
        } else {
            extend(gui)
            extend(Screenshots())
        }
//        extend(TemporalBlur()){
//            samples = 20
//            duration = 1.0
////            colorMatrix = {
////                // `it` is 0.0 at start of frame, 1.0 at end of frame
////                tint(ColorRGBa.WHITE.mix(ColorRGBa.BLUE, it))
////            }
//            gain = 1.2
//            jitter = 0.0
//        }
        extend {
            if (seconds >= settings.loopDuration && RECORD){
                application.exit()
            }
            drawer.fontMap = font
            comp.draw(drawer)
        }
//        extend(FPSDisplay()) {
//            textColor = ColorRGBa.BLACK
//        }
    }
}
