package current

import org.openrndr.applicationSynchronous
import org.openrndr.draw.shadeStyle

fun main() = applicationSynchronous {
    program {
        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    
                """.trimIndent()
            }
            drawer.rectangle(drawer.bounds)
        }
    }
}