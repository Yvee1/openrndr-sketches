import org.intellij.lang.annotations.Language
import org.openrndr.applicationSynchronous
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.extra.glslify.preprocessGlslify

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        val duration = 5.0
        val RECORD = false

        if (RECORD) {
            extend(ScreenRecorder()) {
                frameRate = 50
                profile = GIFProfile()
            }
        }
        extend {
            if (seconds >= duration && RECORD) {
                application.exit()
            }

            drawer.stroke = null
            drawer.shadeStyle = shadeStyle {
                @Language("GLSL")
                val preamble = """
                    #pragma glslify: snoise2 = require(glsl-noise/simplex/2d)
//                    #pragma glslify: snoise3 = require(glsl-noise/simplex/3d)
//                    #pragma glslify: snoise4 = require(glsl-noise/simplex/4d)

                    float ridge(float x, float offset){
                        float n = offset - abs(x);
                        return n * n;
                    }
                    
                    float numOctaves = 4.0;
                    float scale = 5.0;
                    
                    float noise(in vec2 uv){
                        return pow(ridge(snoise2(scale*uv), 0.3), 0.2);
                    }
                    
                    float fbm( in vec2 x, in float H, in float lacunarity )
                    {    
                        float G = exp2(-H);
                        float f = 1.0;
                        float a = 1.0;
                        float t = 0.0;
                        for( int i=0; i<numOctaves; i++ )
                        {
                            t += a*noise(f*x);
                            f *= lacunarity;
                            a *= G;
                        }
                        return t;
                    }
                """.trimIndent()
                @Language("GLSL")
                val frag = """
                    vec2 uv = c_boundsPosition.xy;
//                    x_fill.rgb = vec3(pow(snoise2(scale*uv), snoise2(uv + vec2(160.0+p_time, 1540.0)) + 1.5));
                    x_fill.rgb = vec3(fbm(uv, 2.0, 2.0));
//                    x_fill.rgb = vec3(fbm(uv, 0.5, 1.8));
                """.trimIndent()
                fragmentPreamble = preprocessGlslify(preamble)
                fragmentTransform = frag
                parameter("time", (seconds / duration) % 1)
            }
            drawer.rectangle(drawer.bounds)
        }
    }
}