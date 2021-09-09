package indra

import org.openrndr.WindowMultisample
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.gitarchiver.GitArchiver
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.XYParameter
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.groundPlaneMesh
import org.openrndr.extras.meshgenerators.sphereMesh
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Spherical
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import useful.FPSDisplay
import kotlin.math.*

//private const val RECORD = false
fun main() = application {
    configure {
        multisample = WindowMultisample.SampleCount(512)
        windowResizable = true
    }

    oliveProgram {
        val RECORD = false
        val s = object {
            @DoubleParameter("Translation", -5.0, 5.0)
            var translation = 0.0

            @DoubleParameter("Duration", 0.5, 10.0)
            var duration = 5.0

            @BooleanParameter("Show Riemann sphere")
            var showSphere = true

            @BooleanParameter("Transform")
            var transform = true

            @XYParameter("u", -1.0, 1.0, -1.0, 1.0)
//            var u = Vector2(1.0, 0.0)
            var u = Vector2(-0.65, -0.09)

            @XYParameter("v", -1.0, 1.0, -1.0, 1.0)
            var v = Vector2(0.17, -0.04)
        }
        val gui = GUI()
        gui.add(s)

        val sphere = sphereMesh(64*4, 64*4, 0.5)
        val plane = groundPlaneMesh(1000.0, 1000.0)

        val preamble =  """
                    #define PI 3.14159265359
                    #define cmul(a, b) vec2(a.x*b.x-a.y*b.y, a.x*b.y+a.y*b.x)
                    #define cdiv(a, b) vec2(((a.x*b.x+a.y*b.y)/(b.x*b.x+b.y*b.y)),((a.y*b.x-a.x*b.y)/(b.x*b.x+b.y*b.y)))
                    #define i vec2(0.0, 1.0)
                    #define conj(a) a.yx
                    
                    vec2 csqrt(vec2 a) {
                        float r = length(a);
                        float rpart = sqrt(0.5*(r+a.x));
                        float ipart = sqrt(0.5*(r-a.x));
                        if (a.y < 0.0) ipart = -ipart;
                        return vec2(rpart,ipart);
                    }
                    
                    struct cmat2 {
                        vec2 a;
                        vec2 b;
                        vec2 c;
                        vec2 d;
                    };
                    
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
                    
                    cmat2 mobiusMatrix(vec2 a, vec2 b, vec2 c, vec2 d){
                        vec2 D = cmul(a, d) - cmul(b, c);
                        return cmat2(cdiv(a, csqrt(D)), cdiv(b, csqrt(D)), cdiv(c, csqrt(D)), cdiv(d, csqrt(D)));
                    }
                    
                    cmat2 mobInv(cmat2 mm){
                        return cmat2(mm.d, -mm.b, -mm.c, mm.a);
                    }
                    
                    vec2 mobiusApply(cmat2 mobius, vec2 z){
                        vec2 num = cmul(mobius.a, z) + mobius.b;
                        vec2 den = cmul(mobius.c, z) + mobius.d;
                        return cdiv(num, den);
                    }
                """.trimIndent()

        val render = """
            
            if (p_transform){
    //            complexPos = cdiv(vec2(1.0, 0.0), complexPos);
    //            cmat2 trans = mobiusMatrix(vec2(0.0), vec2(1.0, 0.0), vec2(1.0, 0.0), vec2(0.0));
    //            complexPos = mobiusApply(trans, complexPos);
    
    //            complexPos = cdiv((complexPos - i), (complexPos + i));
    
//                vec2 u = vec2(1.0, 2.0);
    //            cmat2 mm = mobiusMatrix(u, csqrt(cmul(u, u) - 1), csqrt(cmul(u, u) - 1), u);
//                vec2 v = vec2(3.0, 1.0);
                vec2 u = p_u;
                vec2 v = p_v;
                cmat2 mm = mobiusMatrix(u, v, conj(v), conj(u));
                complexPos = mobiusApply(mobInv(mm), complexPos);
            }
            
            float intensity = 0.0;
            intensity += 1.0 - aastep(-0.75 + p_translation, complexPos.x);
            intensity += aastep(0.75 + p_translation, complexPos.x);
            float strokeWidth = 0.02;
            float loc = 0.5;
            intensity += (1 - aastep(loc + strokeWidth, fract(complexPos.y))) * aastep(loc, fract(complexPos.y));
            float arrowWidth = 0.05;
            float arrowHeight = 0.2;
            intensity += 1 - aastep(0.0, sdTriangleIsosceles(rotate2d(PI/2) * vec2(complexPos.x, fract(complexPos.y)) - vec2(loc+strokeWidth/2, -arrowHeight/2.0 - p_translation), vec2(arrowWidth, arrowHeight)));
            vec3 color = vec3(intensity); 
                
            x_fill = vec4(color, 1.0);
        """.trimIndent()

        val cam = Orbital()
        extend(cam) {
            eye = Vector3.UNIT_X * 2.0
            near = 0.001
        }

//        extend(GitArchiver())
        if (!RECORD) {
            extend(Screenshots()){
                multisample = BufferMultisample.SampleCount(512)
            }
            extend(gui)
        } else {
        extend(ScreenRecorder()){
            profile = GIFProfile()
            frameRate = 50
//            frameRate = 60
            maximumDuration = s.duration
            multisample = BufferMultisample.SampleCount(512)
        }
//        extend(TemporalBlur()){
//            jitter = 0.0
//            fps = 50.0
//            duration = 3.5
//            samples = 50
//            multisample = BufferMultisample.SampleCount(512)
//        }
    }

        extend {
            val planeShadeStyle = shadeStyle {
                fragmentPreamble = preamble
                fragmentTransform = """
                vec2 complexPos = va_position.xz;
                """.trimIndent() + render
                parameter("translation", s.translation)
                parameter("transform", s.transform)
                parameter("u", s.u)
                parameter("v", s.v)
            }

            val sphereShadeStyle = shadeStyle {
                fragmentPreamble = preamble
                fragmentTransform = """
                        vec3 spherePos = vec3(va_position.x, va_position.y + 0.5, va_position.z);
                        vec2 complexPos = vec2(spherePos.x, spherePos.z) / (1 - spherePos.y);
                        """.trimIndent() + render
                parameter("translation", s.translation)
                parameter("transform", s.transform)
                parameter("u", s.u)
                parameter("v", s.v)
            }

            drawer.apply {
                clear(ColorRGBa.PINK)
                clear(ColorRGBa.BLACK)
                shadeStyle = planeShadeStyle
                vertexBuffer(plane, DrawPrimitive.TRIANGLES)

                if (s.showSphere) isolated {
                    shadeStyle = sphereShadeStyle
                    translate(0.0, 0.5, 0.0)
                    vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}