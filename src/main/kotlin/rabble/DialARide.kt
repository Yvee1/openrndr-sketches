import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.Drawer
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

//fun main() = applicationSynchronous {
//    configure {
//        width = 800
//        height = 800
//    }
//
//    oliveProgram {
//        data class Tick(val segment: LineSegment, val value: Double)
//        class Axis(var pos: Vector2, var until: Double, var w: Double, var tickDistance: Double = 1.0, var drawLabels: Boolean = true, var drawTicks: Boolean = true,
//                   var colors: List<ColorRGBa> = listOf(rgb(234/255.0,11/255.0,76/255.0), rgb("#00fd7c"), rgb("#4c5ecf"))) {
//            // 1 on the axis maps to scale number of pixels
//            val scale get() = w/until
//            val tickHeight = 10.0
//
//            val ticks: List<Tick> get() {
//                val result = mutableListOf<Tick>()
//                var x = tickDistance*scale
//                while (x <= w){
//                    result.add(Tick(LineSegment(pos.x+x, pos.y, pos.x+x,pos.y+tickHeight), x/scale))
//                    x += tickDistance*scale
//                }
//                x = -tickDistance*scale
//                while (x >= -w){
//                    result.add(Tick(LineSegment(pos.x+x, pos.y, pos.x+x,pos.y+tickHeight), x/scale))
//                    x -= tickDistance*scale
//                }
//                return result.toList()
//            }
//
//            val mainTick = LineSegment(pos.x, pos.y, pos.x, pos.y + tickHeight * 2.0)
//
//            fun draw(drawer: Drawer){
//                drawer.apply {
//                    stroke = colors[0]
//                    strokeWeight = 4.0
//                    lineSegment(pos.x - width, pos.y, pos.x + width, pos.y)
////                    stroke = colors[1]
//                    if (drawTicks) {
//                        strokeWeight = 2.0
//                        lineSegments(ticks.map{it.segment})
//                        if (drawLabels){
//                            texts(ticks.map{it.value.toInt().toString()}, ticks.map{it.segment.end + Vector2(-10.0 + if (it.value.sign >= 0) 6.0 else 0.0, tickHeight*1.5)})
//                        }
//                    }
//
//                    strokeWeight = 3.0
//                    lineSegment(mainTick)
//                }
//            }
//        }
//
//        val ax = Axis(Vector2(width/2.0, height/2.0), 5.0, width/2.0, 1.0)
//        extend {
//            ax.draw(drawer)
//        }
//    }
//}

data class Request(val releaseTime: Double, val pickupLocation: Double, val dropoffLocation: Double)
class Task {
    val requests: List<Request>
    val startTime: Double
    val startLocation: Double

    constructor(n: Int){
        val rs = mutableListOf<Request>()
        var time = 0.0
        for (i in 0 until n) {
            time += Int.uniform(0, 10)
            if (Double.uniform(0.0, 1.0) < 0.5) {
                rs.add(Request(time, 0.0, Int.uniform(-10, 10).toDouble()))
//            rs.add(Request(time, 0.0, Double.uniform(-10.0, 10.0)))
            } else {
//            rs.add(Request(time, Double.uniform(-10.0, 10.0), 0.0))
                rs.add(Request(time, Int.uniform(-10, 10).toDouble(), 0.0))
            }
        }
        requests = rs.toList()
        startTime = 0.0
        startLocation = 0.0
    }

    constructor(st: Double, sl: Double, rs: List<Request>, ){
        requests = rs
        startTime = st
        startLocation = sl
    }
}

fun opt(task: Task): Double {
    if (task.requests.isEmpty()) return task.startTime + abs(task.startLocation)

    val request = task.requests.first()
    val ridingDistance = abs(request.pickupLocation - task.startLocation)
    val readyTime = ridingDistance + task.startTime
    val waitingTime = max(0.0, request.releaseTime - readyTime)
    val newTime = readyTime + waitingTime + abs(request.dropoffLocation - request.pickupLocation)

    return opt(Task(newTime, request.dropoffLocation, task.requests.drop(1)))
}

fun homesick(task: Task): Double {
    if (task.requests.isEmpty()) return task.startTime + abs(task.startLocation)

    val request = task.requests.first()
    val readyTime = max(task.startTime, request.releaseTime)
    val newTime = readyTime + abs(task.startLocation - request.pickupLocation) + abs(request.pickupLocation - request.dropoffLocation) + abs(request.dropoffLocation)

    return homesick(Task(newTime, 0.0, task.requests.drop(1)))
}

val criminal = Task(0.0, 0.0, listOf(Request(10.0, 10.0, 0.0)))

fun main() {
    val worst = List(100000) {
        val task = Task(Int.uniform(1, 5))
        val a = homesick(task)
        val b = opt(task)
        if (a==b) Pair(task, 1.0) else Pair(task, homesick(task)/opt(task))
    }.maxByOrNull{it.second}!!.first
    println(worst.requests)
    println(opt(worst))
    println(homesick(worst))
    println(homesick(worst)/opt(worst))

//    println(Int.uniform(-10, 10).toDouble())
}

//fun main() = println(homesick(getCriminal)/opt(getCriminal))