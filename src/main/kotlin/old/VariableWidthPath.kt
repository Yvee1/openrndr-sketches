import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSVa
import org.openrndr.draw.*
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extras.camera.Orbital
import org.openrndr.shape.path3D
import org.openrndr.extra.shaderphrases.preprocessedFromUrls
import org.openrndr.math.*
import org.openrndr.shape.Path3D

fun main() {
    applicationSynchronous {
        configure {
            width = 800
            height = 800
            multisample = WindowMultisample.SampleCount(8)
        }
        program {
            val cam = Orbital()

            val path = path3D {
                moveTo(Vector3.ZERO)
                for (i in 0 until 100) {
                    continueTo(anchor + Vector3.uniformRing(0.0, 10.0), anchor + Vector3.uniformRing(0.0, 10.0))
                }
            }

            val shader = Shader.preprocessedFromUrls(
                vsUrl = resourceUrl("/shaders/ts-04.vert"),
                tcsUrl = resourceUrl("/shaders/ts-04.tesc"),
                tesUrl = resourceUrl("/shaders/ts-04.tese"),
                gsUrl = resourceUrl("/shaders/ts-04.geom"),
                fsUrl = resourceUrl("/shaders/ts-04.frag")
            )

            drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL
            drawer.depthWrite = true

            extend(cam)
            extend {
                drawer.clear(ColorXSVa(140.0, 0.20, 0.95, 1.0).toRGBa())
                drawVariableWidthPath(this, path, listOf(1.0, 5.0, 10.0), ColorRGBa.BLACK, shader)
            }
        }
    }
}

fun fromV3(v: Vector3, w: Double) = Vector4(v.x, v.y, v.z, w)

fun drawVariableWidthPath(program: Program, path: Path3D, weights: List<Double>, color: ColorRGBa, shader: Shader){
    val vb = vertexBuffer(vertexFormat {
        position(4)
    }, path.segments.size * 4)

    val vc = vb.put {
        for ((i, segment) in path.segments.withIndex()) {
            val cubic = segment.cubic
            write(fromV3(cubic.start, weights[(i*3)%weights.size]))
            write(fromV3(cubic.control[0], weights[(i*3+1)%weights.size]))
            write(fromV3(cubic.control[1], weights[(i*3+2)%weights.size]))
            write(fromV3(cubic.end, weights[((i+1)*3)%weights.size]))
        }
    }

    program.apply {
        shader.begin()
        shader.uniform("offset", mouse.position.xy0)
        shader.uniform("view", drawer.view)
        shader.uniform("proj", drawer.projection)
        shader.uniform("model", drawer.model)
        shader.uniform("resolution", 32)
        shader.uniform("size", 1.0)
        shader.uniform("time", seconds * 1.0)
        shader.uniform("color", color.toVec4())
        drawer.depthWrite = true

        driver.setState(drawer.drawStyle)
        driver.drawVertexBuffer(shader, listOf(vb), DrawPrimitive.PATCHES, 0, vc)
        shader.end()
    }
}

fun ColorRGBa.toVec4() = Vector4(r, g, b, a)