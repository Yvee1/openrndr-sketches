package old

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extras.color.presets.*
import org.openrndr.math.clamp
import org.openrndr.svg.loadSVG
import useful.LatexText
import useful.text
import kotlin.concurrent.thread
import kotlin.math.floor

enum class State {
    Q, H, B, G, W, A, R, D, X, I, K, Z, P, Y, L, S, T, EMPTY
}

fun transition(left: State, middle: State, right: State): State {
    return when(middle){
        State.Q -> {
            val default = when(left){
                State.K -> State.I
                State.S -> State.S
                State.H -> State.H
                State.I -> State.I
                State.W -> State.W
                else -> State.Q
            }
            
            when(right) {
                State.Q -> when(left) {
                    State.Q -> State.Q
                    State.P -> State.S
                    State.X -> State.W
                    State.EMPTY -> State.Q
                    else -> default
                }

                State.K -> when(left) {
                    State.Q -> State.G
                    State.K -> State.K
                    State.L -> State.Z
                    State.A -> State.G
                    State.B -> State.G
                    State.R -> State.G
                    else    -> State.G
                }

                State.P -> when(left) {
                    State.Q -> State.L
                    State.EMPTY -> State.K
                    else -> default
                }

                State.L -> when(left) {
                    State.EMPTY -> State.K
                    else -> State.L
                }

                State.S -> when(left){
                    State.K -> State.X
                    State.X -> State.W
                    else -> default
                }

                State.R -> State.R
                State.G -> State.G

                State.Y -> when(left){
                    State.L -> State.Z
                    else -> default
                }

                State.Z -> when(left){
                    State.K -> State.I
                    State.I -> State.I
                    else -> State.Z
                }

                State.EMPTY -> when(left){
                    State.Q -> State.Q
                    State.P -> State.K
                    State.S -> State.K
                    else -> default
                }

                else -> default
            }
        }

        State.H -> {
            val default = when (left) {
                State.I -> State.I
                else -> State.Q
            }
            when(right) {
                State.Q -> when(left){
                    State.K -> State.I
                    else -> default
                }

                State.K -> when(left) {
                    State.Q -> State.A
                    State.K -> State.K
                    State.B -> State.A
                    State.G -> State.A
                    State.Y -> State.A
                    else -> default
                }

                State.A -> when(left) {
                    State.I -> State.K
                    else -> State.B
                }

                State.B -> when(left){
                    State.I -> State.K
                    else -> State.A
                }

                else -> default
            }
        }

        State.B -> {
            val default = when(left){
                // Mistake in paper? Switched them around
                State.R -> State.B
                else -> State.Q
            }

            when(right) {
                State.Q -> when(left) {
                    State.I -> State.K

                    // Added
//                    State.R -> State.B
                    State.Q -> State.B

                    // Added
                    State.A -> State.B

                    else -> default
                }
                State.A -> when(left){
                    State.I -> State.K

                    // Added
//                    State.H -> State.Q
                    State.Q -> State.B

                    else -> default
                }
                State.R -> State.Q
                State.H -> when(left) {
                    State.I -> State.K

                    // Added
                    State.Q -> State.B

                    else -> default
                }
                State.G -> when(left) {
                    State.Q -> State.K
                    State.A -> State.K
                    State.R -> State.K
                    else -> default
                }
                State.W -> when(left) {
                    State.Q -> State.B
                    else -> default
                }
                else -> default
            }
        }

        State.G -> when(right) {
            State.Q -> when(left) {
                State.A -> State.K
                else -> State.H
            }

            State.K -> when(left) {
                State.A -> State.K
                else -> State.H
            }

            State.H -> when(left) {
                State.B -> State.K
                else -> State.Q
            }
            else -> throw RuntimeException("Whoops\n$left$middle$right")
        }

        State.W -> {
            val default = when(left) {
                State.Q -> State.R
                State.R -> State.Q
//                else -> throw RuntimeException("Whoops")
                // IMPOSSIBLE
                else -> State.EMPTY
            }
            when(right) {
                State.Q -> when(left) {
                    State.A -> State.R
                    State.B -> State.R
                    else -> default
                }
                else -> default
            }
        }

        State.A -> {
            val default = when(left) {
                State.H -> State.H
                else -> State.A
            }

            when(right) {
                State.Q -> when(left) {
                    State.I -> State.K
                    else -> default
                }
                State.K -> when(left) {
                    State.K -> State.K
                    State.I -> State.K
                    else -> default
                }
                State.R -> State.R
                State.G -> when(left) {
                    State.Q -> State.K
                    State.K -> State.K
                    else -> default
                }
                else -> default
            }
        }

        State.R -> {
            val default = when(left) {
                State.A -> State.B
                State.B -> State.A
                else -> State.Q
            }

            when(right) {
                State.K -> when(left) {
                    State.Q -> State.G
                    State.K -> State.K
                    else -> default
                }

                State.B -> when(left) {
                    State.K -> State.A
                    else -> default
                }

                State.G -> when(left) {
                    State.A -> State.K
                    State.B -> State.K
                    else -> State.G
                }

                State.I -> if (left == State.K) State.A else default
                State.X -> if (left == State.K) State.A else default
                else -> default
            }
        }

        State.D -> {
            val default = State.D

            when(right) {
                State.Q -> when(left) {
                    State.K -> State.X
                    State.I -> State.X
                    else -> default
                }

                State.K -> when(left) {
                    State.Q -> State.Y
                    State.K -> State.K
                    State.L -> State.Y
                    else -> default
                }

                State.S -> when(left) {
                    State.K -> State.X
                    else -> default
                }

                State.G -> when(left) {
                    State.Q -> State.Y
                    State.I -> State.K
                    else -> default
                }
                else -> default
            }
        }

        State.X -> when(right) {
            State.Q -> when(left){
                State.Q -> State.A
                State.K -> State.A
                State.R -> State.B
                else -> throw RuntimeException("Whoops\n$left$middle$right")
            }
            else -> throw RuntimeException("Whoops\n$left$middle$right")
        }

        State.I -> {
            val default = when (left) {
                State.Q -> State.R
                State.R -> State.Q
//                else -> throw RuntimeException("Whoops\n$left$middle$right")
                // IMPOSSIBLE
                else -> State.EMPTY
            }

            when(right) {
                State.Q -> when(left) {
                    State.K -> State.R
                    State.A -> State.R
                    else -> default
                }
                State.D -> when(left) {
                    State.K -> State.R
                    else -> default
                }
                State.A -> when(left) {
                    State.Q -> State.K
                    State.K -> State.K
                    else -> default
                }
                State.B -> when (left) {
                    State.K -> State.R
                    State.R -> State.K
                    else -> default
                }
                State.H -> when (left) {
                    State.K -> State.R
                    else -> default
                }
                else -> default
            }
        }

        State.K -> {
            val default = State.K

            when(right) {
                State.K -> when(left) {
                    State.K -> State.T
                    State.EMPTY -> State.T
                    else -> default
                }
                State.EMPTY -> when(left) {
                    State.K -> State.T
                    else -> default
                }
                else -> default
            }
        }

        State.Z -> when(right) {
            State.Q -> State.H
            State.A -> State.H
            State.B -> State.H
            State.H -> State.Q
            else -> throw RuntimeException("Whoops\n$left$middle$right")
        }

        State.P -> when(right) {
            State.Q -> when(left) {
                State.Q -> State.D
                State.EMPTY -> State.K
                else -> throw RuntimeException("Whoops\n$left$middle$right")
            }

            State.EMPTY -> when(left) {
                State.Q -> State.K
                State.EMPTY -> State.T
                else -> throw RuntimeException("Whoops\n$left$middle$right")
            }

            else -> throw RuntimeException("Whoops\n$left$middle$right")
        }

        State.Y -> when(right) {
            State.Q -> State.A
            State.K -> State.A
            State.H -> State.B
            else -> throw RuntimeException("Whoops\n$left$middle$right")
        }

        State.L -> State.Q
        State.S -> State.Q
        State.T -> State.T
        State.EMPTY -> State.EMPTY
//        else -> { default }
    }
}

const val nStates = 50
const val general = 9
const val nSteps = 2 * nStates - 2 - (general - 1)

fun main() = application {
    configure {
        width = 800
        height = 800
        windowResizable = true
    }

    program {
        val epochs: MutableList<Array<State>> = mutableListOf()
        val largeFont = loadFont("data/fonts/default.otf", 64.0)
        val smallFont = loadFont("data/fonts/default.otf", 18.0)

        val current = Array(nStates + 2) {
            when (it) {
                0          -> State.EMPTY
                general    -> State.P
                nStates+1  -> State.EMPTY
                else       -> State.Q
            }
        }

        // Loop until there is a firing state
        epochs.add(current.clone())
        do {
//                for (j in 1 until current.size-1) {
//                    print(current[j])
//                }
//                println()
            val old = current.clone()

            for (j in 1 until current.size - 1){
                current[j] = transition(old[j-1], old[j], old[j+1])
            }
            epochs.add(current.clone())
        } while(!current.any { it == State.T })


        val settings = @Description("Animation settings") object {
            @DoubleParameter("Speed", 0.1, 10.0)
            var speed = 2.0

            @BooleanParameter("Running")
            var running = false

            @IntParameter("Current epoch", 0, nSteps)
            var currentEpoch = 0
        }

        fun stateToColor(state: State): ColorRGBa {
            return when(state) {
                State.P -> ColorRGBa.BLUE
                State.L -> ColorRGBa.PINK
                State.S -> ColorRGBa.PINK
                State.Q -> ColorRGBa.WHITE
                State.D -> ColorRGBa.GREEN
                State.W -> ColorRGBa.BLUE
                State.K -> ColorRGBa.PURPLE
                State.B -> ColorRGBa.DARK_GREEN
                State.G -> ColorRGBa.YELLOW
                State.I -> ColorRGBa.CYAN
                State.A -> ColorRGBa.YELLOW
                State.T -> ColorRGBa.RED
                else -> ColorRGBa.GREY
            }
        }

        fun drawEpoch(drawer: Drawer, epoch: Array<State>){
            val n = epoch.size - 2
            drawer.apply {
                for (i in 1 until epoch.size-1){
                    fill = stateToColor(epoch[i])
                    stroke = ColorRGBa.BLACK
                    fontMap = smallFont
                    val x = i.toDouble()/(n+1) * width
                    circle(x, height/2.0, 10.0)

                    fill = ColorRGBa.BLACK
                    text(LatexText(epoch[i].name, 12.0f),x - 5.0, height/2.0 - 40.0)
                }
            }
        }

        var lastUpdate = 0.0

        keyboard.keyDown.listen {
            if (it.key == KEY_ARROW_LEFT){
                settings.currentEpoch -= 1
            }
            if (it.key == KEY_ARROW_RIGHT){
                settings.currentEpoch += 1
            }
        }

        mouse.buttonDown.listen {
            if (it.button == MouseButton.LEFT) {
//                settings.running = !settings.running
            }
            if (it.button == MouseButton.RIGHT) {
                settings.currentEpoch = 0
            }
            lastUpdate = seconds
        }

        thread {
            application {
                configure {
                    width = 200
                }
                program {
                    val gui = GUI()
                    gui.add(settings)
                    gui.onChange { name, value ->
                        if (name == "currentEpoch") {
                            settings.currentEpoch = (value as Double).toInt().clamp(0, epochs.size - 1)
                        }
                    }
                    gui.doubleBind = true
                    extend(gui)
                }
            }
        }

        extend {
            if (settings.running && seconds - lastUpdate > 1 / settings.speed && settings.currentEpoch < epochs.size-1) {
                settings.currentEpoch += 1
                lastUpdate = seconds
            }
//                drawer.clear(ColorRGBa.WHITE)
            drawer.clear(ColorRGBa.fromHex("#ece6e2"))
//                epochs.getOrNull(settings.currentEpoch)?.let { drawEpoch(drawer, it) }
            drawEpoch(drawer, epochs[settings.currentEpoch.clamp(0, epochs.size-1)])
            drawer.fill = ColorRGBa.BLACK
            drawer.fontMap = largeFont
            drawer.text(settings.currentEpoch.toString(), 10.0, 50.0)
                drawer.text(LatexText("\$t = ${settings.currentEpoch}\$", 18.0f), width/2.0-10.0, height/2.0-150.0)
        }
    }
}