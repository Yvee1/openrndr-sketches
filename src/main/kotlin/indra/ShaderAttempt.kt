package indra

import org.openrndr.WindowMultisample
import org.openrndr.animatable.easing.Easing
import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.gitarchiver.GitArchiver
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.groundPlaneMesh
import org.openrndr.extras.meshgenerators.sphereMesh
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import kotlin.math.*

private const val RECORD = false
fun main() = applicationSynchronous {
    configure {
        multisample = WindowMultisample.SampleCount(512)
//        windowResizable = true
        width = 1000
        height = 1000
    }

    program {
        val s = object {
            @DoubleParameter("Translation", -5.0, 5.0)
            var translation = 0.0

            @DoubleParameter("Duration", 0.5, 10.0)
            var duration = 5.0

            @DoubleParameter("Easing parameter", 1.0, 5.0)
            var k = 2.8

            @DoubleParameter("FOV", 0.0, 180.0)
            var fov = 90.0

            @BooleanParameter("Animate")
            var animate = true
        }
        val gui = GUI()
        gui.add(s)

        val sphere = sphereMesh(64*4, 64*4, 0.5)
        val plane = groundPlaneMesh(1000.0, 1000.0)

        val preamble =  """
                    #define PI 3.14159265359
            
                    float aastep(float threshold, float value) {
                      #ifdef GL_OES_standard_derivatives
                        float afwidth = clamp(length(vec2(dFdx(value), dFdy(value))) * 0.70710678118654757, 0.0, 0.5);
                        return smoothstep(threshold-afwidth, threshold+afwidth, value);
                      #else
                        return step(threshold, value);
                      #endif  
                    }
                    
                    float sdTriangleIsosceles( in vec2 p, in vec2 q )
                    {
                        p.x = abs(p.x);
                        vec2 a = p - q*clamp( dot(p,q)/dot(q,q), 0.0, 1.0 );
                        vec2 b = p - q*vec2( clamp( p.x/q.x, 0.0, 1.0 ), 1.0 );
                        float s = -sign( q.y );
                        vec2 d = min( vec2( dot(a,a), s*(p.x*q.y-p.y*q.x) ),
                                      vec2( dot(b,b), s*(p.y-q.y)  ));
                        return -sqrt(d.x)*sign(d.y);
                    }
                    
                    mat2 rotate2d(float _angle){
                        return mat2(cos(_angle),-sin(_angle),
                                    sin(_angle),cos(_angle));
                    }
                """.trimIndent()

        val render = """
            float intensity = 0.0;
//            intensity += 1.0 - aastep(-0.75 + p_translation, complexPos.x);
//            intensity += aastep(0.75 + p_translation, complexPos.x);
            float strokeWidth = 0.02;
            float loc = 0.4;
            intensity += (1 - aastep(loc + strokeWidth, fract(complexPos.y))) * aastep(loc, fract(complexPos.y));
            float arrowWidth = 0.1;
            float arrowHeight = 0.4;
            intensity += 1 - aastep(0.0, sdTriangleIsosceles(rotate2d(-PI/2) * vec2(complexPos.x, fract(complexPos.y)) - vec2(loc+strokeWidth, 1.0 + p_translation), vec2(arrowWidth, arrowHeight)));
            vec3 color = vec3(intensity); 
                
            x_fill = vec4(color, 1.0);
        """.trimIndent()

        val cam = Orbital()
//        cam.camera.
        extend(cam) {
//            eye = Vector3.UNIT_X * 2.0
//            eye = Vector3(x=1.730467138510836, y=1.0026740264371863, z=0.011326042661080268)
            eye = Vector3(x = sqrt(3.0), y = 1.0, z = 0.0)
            near = 0.001
        }
        extend(GitArchiver())
        if (!RECORD) {
            extend(Screenshots()){
                multisample = BufferMultisample.SampleCount(512)
            }
            extend(gui)
        } else {
        extend(ScreenRecorder()){
            profile = GIFProfile()
            frameRate = 50
            maximumDuration = s.duration
            multisample = BufferMultisample.SampleCount(512)
        }
        extend(TemporalBlur()){
            jitter = 0.0
            fps = 50.0
            duration = 3.5
            samples = 50
            multisample = BufferMultisample.SampleCount(512)
        }
    }

        val oldPhi = cam.camera.spherical.phi
        val oldTheta = cam.camera.spherical.theta

        fun easingK(x: Double, k: Double) = if (x < 0.5) 2.0.pow(k-1)*x.pow(k) else 1 - (-2 * x + 2).pow(k) / 2
        fun easing(x: Double) = easingK(x, s.k)
//        fun easing(x: Double) = Easing.CubicInOut.easer.ease(x, 0.0, 1.0, 1.0)

        extend {
            cam.camera.fov = s.fov

            if (s.animate) {
                val t = easing((seconds / s.duration) % 1)
                val imp = (sin(t * 2 * PI) + 0.5 * sin(t * 3 * PI)) * 1.5
//            val imp = sin(t * 2 * PI) + 0.5*sin(t * 4 * PI) + 0.25*cos(t * 4 * PI) - 0.25
                val pos = Spherical(oldTheta + t * 360.0, oldPhi + imp * 10.0, cam.camera.spherical.radius)
                cam.camera.setView(cam.camera.lookAt, pos, cam.camera.fov)
            }

//            print(cam.camera.spherical.cartesian.toString() + "\r")
            val planeShadeStyle = shadeStyle {
                fragmentPreamble = preamble
                fragmentTransform = """
                vec2 complexPos = va_position.xz;
                """.trimIndent() + render
                parameter("translation", s.translation)
            }

            val sphereShadeStyle = shadeStyle {
                fragmentPreamble = preamble
                fragmentTransform = """
                        vec3 spherePos = vec3(va_position.x, va_position.y + 0.5, va_position.z);
                        vec2 complexPos = vec2(spherePos.x, spherePos.z) / (1 - spherePos.y);
                        """.trimIndent() + render
                parameter("translation", s.translation)
            }

            drawer.apply {
                clear(ColorRGBa.PINK)
                clear(ColorRGBa.BLACK)
                shadeStyle = planeShadeStyle
                vertexBuffer(plane, DrawPrimitive.TRIANGLES)
                isolated {
                    shadeStyle = sphereShadeStyle
                    translate(0.0, 0.5, 0.0)
                    vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}