package current

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.drawComposition
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.gitarchiver.GitArchiver
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extras.color.presets.BEIGE
import org.openrndr.extras.color.presets.BLUE_STEEL
import org.openrndr.extras.color.presets.ORANGE
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.*
import org.openrndr.svg.saveToFile
import useful.*
import java.io.File
import kotlin.math.max
import kotlin.math.min

fun min(vararg input: Double): Double = input.minOrNull()!!
fun Double.format(digits: Int) = "%.${digits}f".format(this)

fun discreteFrechet(p: List<Vector2>, q: List<Vector2>) : Array<Array<Double>>  {
    val n = p.size
    val m = q.size
    val gamma: Array<Array<Double>> = Array(n) { Array(m) { Double.POSITIVE_INFINITY } }

    fun d(i: Int, j: Int) = p[i].distanceTo(q[j])

    // Base cases
    for (j in 0 until m){
        gamma[0][j] = d(0, j)
    }
    for (i in 1 until n){ // start from 1 because [0][0] was calculated in previous loop
        gamma[i][0] = d(i, 0)
    }

    // Recurrence
    for (i in 1 until n){
        for (j in 1 until m){
            gamma[i][j] = max(d(i, j), min(gamma[i-1][j-1], gamma[i-1][j], gamma[i][j-1]))
        }
    }

    return gamma
}

fun discreteFrechetLimited(p: List<Vector2>, q: List<Vector2>, l: Int) : Array<Array<Array<Double>>>  {
    val n = p.size
    val m = q.size
    val gamma: Array<Array<Array<Double>>> = Array(n) { Array(m) { Array(2*l+1) { Double.POSITIVE_INFINITY } }}
    // -l, -l+1, ..., -1 , 0, 1  , ..., l-1 , l
    //  0,   1 , ..., l-1, l, l+1, ..., 2l-1, 2l

    fun d(i: Int, j: Int) = p[i].distanceTo(q[j])

    // Base cases
    gamma[0][0][l] = d(1, 1)
    for (j in 1 until m){
        if (j <= l) {
            gamma[0][j][l-j] = max(d(0, j), gamma[0][j-1][l-j+1])
        }
    }
    for (i in 1 until n){
        if (i <= l) {
            gamma[i][0][l+i] = max(d(i, 0), gamma[i-1][0][l+i-1])
        }
    }

    // Recurrence
    for (i in 1 until n){
        for (j in 1 until m){
            for (k in 0 until 2*l+1) {
                gamma[i][j][k] = max(d(i, j), when {
                    k < l  -> gamma[i][j-1][k+1]
                    k == l -> List(2*l+1){
                            gamma[i-1][j-1][it]
                        }.minOrNull()!!
                    else -> gamma[i-1][j][k-1]
                })
            }
        }
    }

    return gamma
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

fun determineMappingLimited(gamma: Array<Array<Array<Double>>>): List<Pair<Int, Int>> {
    val l = (gamma[0][0].size-1)/2
    // start "top right"
    var j = gamma[0].size - 1
    var i = gamma.size - 1
    var k = gamma[i][j].withIndex().minByOrNull { (_, v) -> v }!!.index

    val result = mutableListOf(Pair(i, j))

    while (i > 0 && j > 0){
        println("i: ${i}, j: ${j}, k: ${k}")
        when {
            k < l -> {
                result.add(Pair(i, j - 1))
                j -= 1
                k += 1
            }
            k == l -> {
                result.add(Pair(i-1, j-1))
                k = List(2*l+1){
                    gamma[i-1][j-1][it]
                }.withIndex().minByOrNull { (_, v) -> v }!!.index
                i -= 1
                j -= 1
            }
            else -> {
                result.add(Pair(i - 1, j))
                i -= 1
                k -= 1
            }
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
//    val p = listOf(1.0 v2 0.0, 1.5 v2 0.3, 2.0 v2 1.0, 3.0 v2 0.5, 4.0 v2 -1.0, 5.0 v2 0.0)
//    val q =  listOf(1.0 v2 0.5, 2.0 v2 0.8, 3.0 v2 0.5, 4.0 v2 0.3, 5.0 v2 0.0)

//    val p = listOf(1.0 v2 0.0, 1.5 v2 0.3, 2.0 v2 1.0, 3.0 v2 0.5, 4.0 v2 -1.0, 5.0 v2 0.0)

    application {
        configure {
            windowResizable = true
            width = 1200
            height = 600
        }

        program {
            fun Drawer.drawPolyline(polyline: List<Vector2>){
                fill = null
                lineStrip(polyline)
                fill = ColorRGBa.WHITE
                strokeWeight /= 2.0
                circles(polyline, 0.020)
                strokeWeight *= 2.0
            }

            fun CompositionDrawer.drawPolyline(polyline: List<Vector2>){
                fill = null
                lineStrip(polyline)
                fill = ColorRGBa.WHITE
                strokeWeight /= 2.0
                circles(polyline, 0.020)
                strokeWeight *= 2.0
            }

            fun Drawer.drawLabels(polyline: List<Vector2>, name: String){
                for (i in polyline.indices){
                    val n = if (i == 0) (polyline[1] - polyline[0]).perpendicular() else (polyline[i] - polyline[i-1]).perpendicular()
                    val pos = polyline[i] + n * 0.2
                    text(LatexText("$${name}_$i$", 14.0), pos.x, pos.y)
                }
            }

            fun CompositionDrawer.drawLabels(polyline: List<Vector2>, name: String) {
                isolated {
                    scale(1 / 100.0, -1 / 100.0)
                    strokeWeight /= 100.0
                    fill = ColorRGBa.BLACK

                    for (i in polyline.indices) {
                        val n = if (i == 0) (polyline[1] - polyline[0]).perpendicular()
                                else (polyline[i] - polyline[i - 1]).perpendicular()
                        val pos = (polyline[i] * (100.0 v2 -100.0)) + n * 10.0
                        circle(pos, 3.0)
                        text(LatexText("$${name}_$i$", 14.0), pos.x, pos.y)
                    }
                    strokeWeight *= 100.0
                }
            }

            fun Drawer.drawMapping(mapping: List<Pair<Int, Int>>, p: List<Vector2>, q: List<Vector2>){
                lineSegments(mapping.map { pair -> LineSegment(p[pair.first], q[pair.second]) })
            }

            fun CompositionDrawer.drawMapping(mapping: List<Pair<Int, Int>>, p: List<Vector2>, q: List<Vector2>){
                lineSegments(mapping.map { pair -> LineSegment(p[pair.first], q[pair.second]) })
            }

            val viridis = Viridis()
            fun Drawer.drawDPTable(table: Array<Array<Double>>, pos: Vector2, size: Double){
                val n = table.size
                val m = table[0].size

                val cellSize = size / max(n, m)
                val largest: Double = table.map { it.maxOrNull()!! }.maxOrNull()!!

                val rBatch = rectangleBatch {
                    stroke = null
                    for (i in 0 until n) {
                        for (j in 0 until m) {
                            val t = table[i][j]/largest
//                            fill = ColorRGBa.BLACK.opacify(1-t)
                            fill = viridis.getColor(t)
                            val r = Rectangle(j * cellSize, i * cellSize, cellSize, cellSize) as Rectangle
                            rectangle(r)
                        }
                    }
                }

                isolated {
                    translate(pos)

                    // Colored cells
                    rectangles(rBatch)

                    stroke = ColorRGBa.BLACK
                    fill = null
                    val w = m*cellSize
                    val h = n*cellSize

                    // Grid lines
                    strokeWeight /= 2.0
                    for (i in 1 until n) {
                        lineSegment(0.0, i * cellSize, w, i * cellSize)
                    }
                    for (j in 1 until m) {
                        lineSegment(j * cellSize, 0.0, j*cellSize, h)
                    }
                    strokeWeight *= 2.0

                    // Outer rectangle
                    contour(Rectangle(0.0, 0.0, w, h).contour)

                    // Color bar
                    viridis.run {
                        drawColorBar(-(0.5 v2 0.0), 0.2, h)
                    }

                    strokeWeight /= 2
                    contour(Rectangle(-0.5, 0.0, 0.2, h).contour)
                    strokeWeight *= 2

                    isolated {
                        scale(1 / 100.0, -1 / 100.0)
//                        strokeWeight *= 100.0
//                        strokeWeight /= 3
//                        lineSegment(-0.55*100.0, 0.0, -0.5*100.0, 0.0)
//                        text(LatexText("$0$", 14.0f), -0.75*100.0, -12.0)
//                        lineSegment(-0.55*100.0, -h*100.0, -0.5*100.0, -h*100.0)
//                        text(LatexText("$${largest.format(1)}$", 14.0f), -0.85*100.0, -h*100.0-12.0)
//
//                        strokeWeight *= 3
//                        strokeWeight /= 100.0

//                        text(LatexText("$0$", 14.0), -0.70*100.0, -12.0)
//                        text(LatexText("$${largest.format(1)}$", 14.0), -0.80*100.0, -h*100.0-12.0)
                    }
                }
            }

            fun CompositionDrawer.drawDPTable(table: Array<Array<Double>>, pos: Vector2, size: Double){
                val n = table.size
                val m = table[0].size

                val cellSize = size / max(n, m)
                val largest: Double = table.map { row -> row.map { if (it == Double.POSITIVE_INFINITY) 0.0 else it }  }.map { it.maxOrNull()!! }.maxOrNull()!!

                isolated {
                    translate(pos)

                    // Colored cells
                    stroke = null
                    for (i in 0 until n) {
                        for (j in 0 until m) {
                            val value = table[i][j]
                            if (value == Double.POSITIVE_INFINITY){
                                fill = ColorRGBa.WHITE
                            } else {
                                val t = table[i][j] / largest
                                fill = viridis.getColor(t)
                            }
                            val r = Rectangle(j * cellSize, i * cellSize, cellSize, cellSize) as Rectangle
                            rectangle(r)
                        }
                    }

                    stroke = ColorRGBa.BLACK
                    fill = null
                    val w = m*cellSize
                    val h = n*cellSize

                    // Grid lines
                    strokeWeight /= 2.0
                    for (i in 1 until n) {
                        lineSegment(0.0, i * cellSize, w, i * cellSize)
                    }
                    for (j in 1 until m) {
                        lineSegment(j * cellSize, 0.0, j*cellSize, h)
                    }
                    strokeWeight *= 2.0

                    // Outer rectangle
                    contour(Rectangle(0.0, 0.0, w, h).contour)

                    // Color bar
                    viridis.run {
                        drawColorBar(-(0.5 v2 0.0), 0.2, h)
                    }

                    strokeWeight /= 2
                    contour(Rectangle(-0.5, 0.0, 0.2, h).contour)
                    strokeWeight *= 2

                    isolated {
                        scale(1 / 100.0, -1 / 100.0)
//                        strokeWeight *= 100.0
//                        strokeWeight /= 3
//                        lineSegment(-0.55*100.0, 0.0, -0.5*100.0, 0.0)
//                        text(LatexText("$0$", 14.0f), -0.75*100.0, -12.0)
//                        lineSegment(-0.55*100.0, -h*100.0, -0.5*100.0, -h*100.0)
//                        text(LatexText("$${largest.format(1)}$", 14.0f), -0.85*100.0, -h*100.0-12.0)
//
//                        strokeWeight *= 3
                        strokeWeight /= 100.0
                        fill = ColorRGBa.BLACK

//                        text(LatexText("$0$", 14.0), -0.70*100.0, -12.0)
//                        text(LatexText("$${largest.format(1)}$", 14.0), -0.80*100.0, -h*100.0-12.0)
                    }
                }
            }

            fun Drawer.drawDPPath(table: Array<Array<Double>>, pos: Vector2, size: Double, mapping: List<Pair<Int, Int>>){
                val n = table.size
                val m = table[0].size
                val cellSize = size / max(n, m)

                isolated {
                    translate(pos)
                    lineStrip(mapping.map { pair -> Vector2((pair.second+0.5)  * cellSize, (pair.first+0.5) * cellSize) })
                }
            }

            fun CompositionDrawer.drawDPPath(table: Array<Array<Double>>, pos: Vector2, size: Double, mapping: List<Pair<Int, Int>>){
                val n = table.size
                val m = table[0].size
                val cellSize = size / max(n, m)

                isolated {
                    translate(pos)
                    lineStrip(mapping.map { pair -> Vector2((pair.second+0.5)  * cellSize, (pair.first+0.5) * cellSize) })
                }
            }

//            val q = listOf(1.0 v2 0.0, 2.0 v2 0.5, 2.0 v2 -0.5)
            val pp = listOf(0.5 v2 0.0, 0.6 v2 -0.2, 0.7 v2 -0.35, 0.8 v2 -0.45, 1.0 v2 -0.5, 1.3 v2 -0.35)
            val p = ShapeContour(pp.zipWithNext { a, b -> Segment(a, b) }, false).equidistantPositions(20)
//            val q = listOf(1.0 v2 0.0, 1.3 v2 0.2, 1.7 v2 0.4, 2.0 v2 0.5, 2.5 v2 0.0, 2.8 v2 -0.5)
            val qq = listOf(1.0 v2 0.0, 2.0 v2 0.5, 2.3 v2 0.3,  2.5 v2 0.0, 2.6 v2 -0.3, 2.6 v2 -0.5)
            val q = ShapeContour(qq.zipWithNext { a, b -> Segment(a, b) }, false).equidistantPositions(20)

            val gamma = discreteFrechet(p, q)
            val gammaLimited = discreteFrechetLimited(p, q, 2)
            val gammaLimitedFlattened = gammaLimited.map { row -> row.map { cell -> cell.minOrNull()!! }.toTypedArray() }.toTypedArray()

//            for (row in gamma.reversed()){
//                for ((i, value) in row.withIndex()){
//                    print("${value!!.format(2)}\t")
//                    if (i == row.size-1) println()
//                }
//            }

            val mapping = determineMapping(gamma)
            val mappingLimited = determineMappingLimited(gammaLimited)

//            println(gammaLimited)
            println(mappingLimited)

            val row = gammaLimited.last().last()
            for ((i, value) in row.withIndex()){
                print("${value!!.format(2)}\t")
                if (i == row.size-1) println()
            }

            val trans = Matrix44(1.771561000000001, 0.0, 0.0, -384.9438670000003,
                0.0, 1.771561000000001, 0.0, -334.51698400000015,
                0.0, 0.0, 1.771561000000001, 0.0,
                0.0, 0.0, 0.0, 1.0)

            val comp = drawComposition {
                strokeWeight *= 3

                model *= trans
                translate(width/2.0, height/2.0)
                scale(100.0)
                scale(1.0, -1.0)
//                    translate(-2.5, 0.0)
//                strokeWeight /= 50.0

                isolated {
                    translate(0.0, -1.5)
                    strokeWeight /= 2.0
                    stroke = ColorRGBa.PINK
                    drawMapping(mapping, p, q)
                    strokeWeight *= 2.0
                    stroke = ColorRGBa.BLACK
//                    stroke = ColorRGBa.ORANGE
                    drawPolyline(p)
//                    drawLabels(p, "p")
//                    stroke = ColorRGBa.BLUE_STEEL
                    drawPolyline(q)
//                    drawLabels(q, "q")
                }

                val pos = Vector2(-3.0, -2.1)
                val size = 3.0
                drawDPTable(gamma, pos, size)
//                drawDPTable(gammaLimitedFlattened, pos + (0.0 v2 size*1.5), size)

                isolated {
//                    stroke = ColorRGBa.fromHex("E7ECEF")
                    stroke = ColorRGBa.PINK
                    translate(-0.01, -0.01)
                    drawDPPath(gamma, pos, size, mapping)
                }
                isolated {
//                        stroke = ColorRGBa.fromHex("390040")
//                    stroke = ColorRGBa.fromHex("FF8811")
                    stroke = ColorRGBa.BEIGE.shade(.6)
                    translate(0.01, 0.01)
                    drawDPPath(gamma, pos, size, mappingLimited)
                }

                isolated {
                    translate(0.0, 0.25)
                    strokeWeight /= 2.0
//                    stroke = ColorRGBa.fromHex("FF8811")
                    stroke = ColorRGBa.BEIGE.shade(.6)
                    drawMapping(mappingLimited, p, q)
                    strokeWeight *= 2.0
                    stroke = ColorRGBa.BLACK
//                    stroke = ColorRGBa.ORANGE
                    drawPolyline(p)
//                    stroke = ColorRGBa.BLUE_STEEL
                    drawPolyline(q)
                }
            }
            comp.saveToFile(File("teeeeeeesssssttttt.svg"))

            extend(GitArchiver())
            extend(Screenshots())
            extend(FPSDisplay())
            val trs = TransRotScale()
            extend(trs){
//                viewMat =
            }
            extend {
                drawer.fontMap = loadFont("data/fonts/default.otf", 18.0)
//                println(trs.viewMat)
                drawer.clear(ColorRGBa.WHITE)
//                drawer.composition(comp)
                drawer.apply {

//                    strokeWeight *= 3

                    model *= trans
                    translate(width / 2.0, height / 2.0)
                    scale(100.0)
                    scale(1.0, -1.0)
//                    translate(-2.5, 0.0)
                    strokeWeight /= 100.0

                    isolated {
                        translate(0.0, -1.5)
                        strokeWeight /= 2.0
                        stroke = ColorRGBa.PINK
                        drawMapping(mapping, p, q)
                        strokeWeight *= 2.0
                        stroke = ColorRGBa.BLACK
//                    stroke = ColorRGBa.ORANGE
                        drawPolyline(p)
//                    drawLabels(p, "p")
//                    stroke = ColorRGBa.BLUE_STEEL
                        drawPolyline(q)
//                    drawLabels(q, "q")
                    }

                    val pos = Vector2(-3.0, -2.1)
                    val size = 3.0
                    drawDPTable(gamma, pos, size)
//                drawDPTable(gammaLimitedFlattened, pos + (0.0 v2 size*1.5), size)

                    isolated {
//                    stroke = ColorRGBa.fromHex("E7ECEF")
                        stroke = ColorRGBa.PINK
                        translate(-0.01, -0.01)
                        drawDPPath(gamma, pos, size, mapping)
                    }
                    isolated {
//                        stroke = ColorRGBa.fromHex("390040")
//                    stroke = ColorRGBa.fromHex("FF8811")
                        stroke = ColorRGBa.BEIGE.shade(.6)
                        translate(0.01, 0.01)
                        drawDPPath(gamma, pos, size, mappingLimited)
                    }

                    isolated {
                        translate(0.0, 0.25)
                        strokeWeight /= 2.0
//                    stroke = ColorRGBa.fromHex("FF8811")
                        stroke = ColorRGBa.BEIGE.shade(.6)
                        drawMapping(mappingLimited, p, q)
                        strokeWeight *= 2.0
                        stroke = ColorRGBa.BLACK
//                    stroke = ColorRGBa.ORANGE
                        drawPolyline(p)
//                    stroke = ColorRGBa.BLUE_STEEL
                        drawPolyline(q)
                    }
                }
            }
        }
    }
}

//fun Drawer.zoom()