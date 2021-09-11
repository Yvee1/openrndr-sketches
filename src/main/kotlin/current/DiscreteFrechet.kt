package current

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extensions.Screenshots
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import useful.NativeGitArchiver
import kotlin.math.max
import kotlin.math.min

fun min(vararg input: Double): Double = input.minOrNull()!!
fun Double.format(digits: Int) = "%.${digits}f".format(this)

fun discreteFrechet(p: List<Vector2>, q: List<Vector2>) : Array<Array<Double>>  {
    val n = p.size
    val m = q.size
    val gamma: Array<Array<Double?>> = Array(n) { arrayOfNulls(m) }

    fun d(i: Int, j: Int) = p[i].distanceTo(q[j])

    // Base cases
    for (j in 0 until m){
        gamma[0][j] = d(0, j)
    }
    for (i in 1 until n){ // start from 1 because [0][0] was calculated in previous loop
        gamma[i][0] = d(i, 0)
    }

    val path = mutableListOf(Pair(n-1, m-1))

    // Recurrence
    for (i in 1 until n){
        for (j in 1 until m){
            gamma[i][j] = max(d(i, j), min(gamma[i-1][j-1]!!, gamma[i-1][j]!!, gamma[i][j-1]!!))
        }
    }

    return gamma.map { it.requireNoNulls() }.toTypedArray()
}

fun determineMapping(gamma: Array<Array<Double>>): List<Pair<Int, Int>> {
    var j = gamma[0].size - 1
    var i = gamma.size - 1

    val result = mutableListOf(Pair(i, j))

    while (i > 0 && j > 0){
        val value = gamma[i][j]
        if (gamma[i-1][j-1] <= value){
            result.add(Pair(i-1, j-1))
            i--
            j--
        } else if (gamma[i][j-1] <= value){
            result.add(Pair(i, j-1))
            j--
        } else {
            result.add(Pair(i-1, j))
            i--
        }
    }

    if (result.last().first > 0){
        for (i in result.last().first-1 downTo 0){
            result.add(Pair(i, 0))
        }
    }

    if (result.last().second > 0){
        for (j in result.last().second-1 downTo 0){
            result.add(Pair(0, j))
        }
    }

    return result.reversed()
}

infix fun Double.v2(other: Double): Vector2 = Vector2(this, other)

fun main() {
    val p = listOf(1.0 v2 0.0, 1.5 v2 0.3, 2.0 v2 1.0, 3.0 v2 0.5, 4.0 v2 -1.0, 5.0 v2 0.0)
    val q =  listOf(1.0 v2 0.5, 2.0 v2 0.8, 3.0 v2 0.5, 4.0 v2 0.3, 5.0 v2 0.0)

    val gamma = discreteFrechet(p, q)

    for (row in gamma.reversed()){
        for ((i, value) in row.withIndex()){
            print("${value!!.format(2)}\t")
            if (i == row.size-1) println()
        }
    }

    val mapping = determineMapping(gamma)
    println(mapping)

    application {
        configure {
            windowResizable = true
        }

        program {
            fun drawPolyline(polyline: List<Vector2>){
                drawer.apply {
                    stroke = ColorRGBa.BLACK
                    fill = ColorRGBa.WHITE
                    lineStrip(polyline)
                    circles(polyline, 0.1)
                }
            }

            fun drawMapping(mapping: List<Pair<Int, Int>>){
                drawer.apply {
                    stroke = ColorRGBa.PINK
                    lineSegments(mapping.map { pair -> LineSegment(p[pair.first], q[pair.second]) })
                }
            }

            extend(NativeGitArchiver())
            extend(Screenshots())
            extend {
                drawer.apply {
                    clear(ColorRGBa.WHITE)
                    translate(width/2.0, height/2.0)
                    scale(100.0)
                    translate(-2.5, 0.0)
                    strokeWeight /= 50.0

                    drawMapping(mapping)
                    drawPolyline(p)
                    drawPolyline(q)
                }
            }
        }
    }
}

//fun Drawer.zoom()