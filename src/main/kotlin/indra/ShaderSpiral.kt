package indra

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.persistent
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.sphereMesh
import org.openrndr.math.Vector3
import useful.FPSDisplay
import kotlin.concurrent.thread
import kotlin.math.PI

fun main() = application {
    configure {
        width = 500
        height = 500
        windowAlwaysOnTop = true
        windowResizable = true
    }

    oliveProgram {
        val gui = GUI()
        val s = object {
            @DoubleParameter("Repeat", 0.0, 7.0)
            var repeat = 1.0

            @DoubleParameter("Angle", 0.0, PI)
            var angle = 1.4142
        }
        gui.add(s)

        val preamble = """
            const float a=1.0;
            const float b=.1759;
            const float PI=3.14159265359;
            
//            float spiralSDF(vec2 p,vec2 c){
//                p = p - c;
//                float t=atan(p.y, p.x); //+ p_time*8.0;
//                float r=length(p.xy);
//                
//                float n=(log(r/a)/b-t)/(2.*PI);
//                float upper_r=a*exp(b*(t+2.*PI*ceil(n)));
//                float lower_r=a*exp(b*(t+2.*PI*floor(n)));
//                
//                return min(abs(upper_r-r),abs(r-lower_r));
//            }

            vec2 clog(vec2 z){
                return vec2(log(length(z)), atan(z.y, z.x));
            }

//            float spiralSDF(vec2 p){
//                float t = atan(p.y, p.x); //+ p_time*8.0;
//                float r = length(p.xy);
//                
//                
//                float n=(log(r/a)/b-t)/(2.*PI);
//                float upper_r=a*exp(b*(t+2.*PI*ceil(n)));
//                float lower_r=a*exp(b*(t+2.*PI*floor(n)));
//                
//                return min(abs(upper_r-r),abs(r-lower_r));
//            }
            
            float lineSDF(vec2 O, vec2 dir, vec2 P)
            {
                vec2 D = normalize(dir);
                vec2 X = O + D * dot(P-O, D);

                return distance(P, X);
            }
            
            float hAxis(vec2 p, float sw){
                return abs(p.y) - sw;
//                return 1.0;
            }
            
            float aastep(float threshold, float value) {
              #ifdef GL_OES_standard_derivatives
                float afwidth = clamp(length(vec2(dFdx(value), dFdy(value))) * 0.70710678118654757, 0.0, 0.5);
                return smoothstep(threshold-afwidth, threshold+afwidth, value);
              #else
                return step(threshold, value);
              #endif  
            }
            
            mat2 rotate2d(float _angle){
                return mat2(cos(_angle),-sin(_angle),
                            sin(_angle),cos(_angle));
            }
        """.trimIndent()

        val render = """
//            float intensity = aastep(0.01, spiralSDF(complexPos, vec2(0.0, 0.0)));
//            float intensity = spiralSDF(complexPos, vec2(0.0, 0.0));
//            complexPos = 
            complexPos *= 30.0;
//            complexPos *= 490.0;
//            complexPos *= rotate2d(p_time);
            complexPos = clog(complexPos*1.0);
            complexPos = rotate2d(p_angle) * complexPos;
            
//            complexPos *= 50.0;
            float intensity;
//            intensity = aastep(0.05, lineSDF(vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(complexPos.x, mod(complexPos.y, 2*PI))));
//            intensity = aastep(0.1, lineSDF(vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(complexPos.x, complexPos.y)));
            intensity = aastep(0.0, hAxis(vec2(complexPos.x, -0.5 + mod(complexPos.y, p_repeat)), 0.02));
            vec3 color = vec3(intensity);
//            vec3 color = vec3(1.0);
//            color *= vec3(vec2(complexPos.x, mod(complexPos.y, 2*PI) / (2*PI)), 0.0);
//            x_fill = vec4(color, 1.0);
//            x_fill = vec4(vec3(1.0), intensity);
            x_fill = vec4(1.0, 1.0, 1.0, 1.0-intensity);
            
        """.trimIndent()

//        thread {
//            application {
//                configure {
//                    width = 500
//                    height = 500
//                    windowAlwaysOnTop = true
//                    multisample = WindowMultisample.SampleCount(8)
//                }
//
//                program {
//                    val sphere = sphereMesh(64 * 4, 64 * 4, 0.5)
//
//                    extend(Orbital()) {
//                        eye = Vector3.UNIT_X * 2.0
////                        near = 0.0001
////                        far = 10000.0
//                    }
//                    extend {
//                        drawer.isolated {
//                            clear(ColorRGBa.PINK)
//                            shadeStyle = shadeStyle {
//                                fragmentPreamble = preamble
//                                fragmentTransform = """
//                        vec3 spherePos = vec3(va_position.x, va_position.y + 0.5, va_position.z);
//                        vec2 complexPos = vec2(spherePos.x, spherePos.z) / (1 - spherePos.y);
//                    """.trimIndent() + render
//                                parameter("time", seconds)
            //                    parameter("repeat", s.repeat)
            //                    parameter("angle", s.angle)
//                            }
////                            translate(0.0, 0.5, 0.0)
//                            vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
//                        }
//                    }
//                }
//            }
//        }

        extend(FPSDisplay()) {
            textColor = ColorRGBa.BLACK
        }
        extend(gui)
        extend {
            drawer.apply {
                shadeStyle = shadeStyle {
                    fragmentPreamble = preamble
                    fragmentTransform = """
                        float scale = 1.0;
                        vec2 complexPos = scale * (va_position.xy - vec2(0.5));
                    """.trimIndent() + render
                    parameter("time", seconds)
                    parameter("repeat", s.repeat)
                    parameter("angle", s.angle)
                }
                stroke = null
                rectangle(bounds)
            }
        }
    }
}