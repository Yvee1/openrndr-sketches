package useful

import org.openrndr.ApplicationDslMarker
import org.openrndr.Configuration
import org.openrndr.Program

@ApplicationDslMarker
class CustomBuilder constructor(val configuration: Configuration, var program: Program) {
    fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    fun program(init: suspend Program.() -> Unit) {
        program = object : Program() {
            override suspend fun setup() {
                init()
            }
        }
    }
}