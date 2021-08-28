import org.intellij.lang.annotations.Language
import org.openrndr.applicationSynchronous
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import kotlin.math.PI

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }

    program {
        // https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Kissing_Prairie_dog_edit_3.jpg/1024px-Kissing_Prairie_dog_edit_3.jpg
        val img = loadImage("data/images/flowerTile.png")
        val duration = 1.5
        val RECORD = true

        if (RECORD) {
            extend(ScreenRecorder()) {
                frameRate = 50
                profile = GIFProfile()
            }
        }
        extend {
            if (seconds >= duration && RECORD){
                application.exit()
            }

            drawer.stroke = null
            drawer.shadeStyle = shadeStyle {
                fragmentPreamble = """
                """.trimIndent()
                @Language("GLSL")
                val frag = """
                    vec2 uv = c_boundsPosition.xy;
                    uv.y = 1.-uv.y; // vert flip
                    vec2 I = uv;
                    float s = 0.2;
                    float t = 0.0001;
                    I -= 0.5;
                    //float rotation = p_time * 6.283;
                    //I *= mat2(cos(rotation), sin(rotation), -sin(rotation), cos(rotation));
                    I = vec2((1./t/10.0*0.0)*p_time, p_time - s * log2(length(I))) + atan(I.y, I.x) / 6.283; 
                    I.x = ceil(I.y) - I.x;
                    I.x *= t; // approximation to golden angle pi*(3 - sqrt(5))

                    I = fract(I);
                    vec3 O = vec3(I, 0.);
                    O = texture(p_img, fract(I)).xyz;
                    x_fill.rgb = O;
                """.trimIndent()
                fragmentTransform = frag
                parameter("img", img)
                parameter("time", (seconds / duration) % 1)
            }
            drawer.rectangle(drawer.bounds)
//            print("\r" + (seconds/duration % 1).toString())
        }
    }
}