import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.Random.gaussian
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        val duration = 3.0

        data class GridCell(val pos: Vector2, val row: Int, val column: Int)

        val grid = mutableListOf<GridCell>()
        val margin = 100
        val gap = 10
//        for (x in margin + gap until width - margin step gap){
//            for (y in margin + gap until height - margin step gap){
//                grid.add(Vector2(x.toDouble(), y.toDouble()))
//            }
//        }
        val startX = margin + gap
        val startY = margin + gap
        val endX = width - margin
        val endY = height - margin

        val nrColumns = (endX - startX) / gap
        val nrRows = (endY - startY) / gap

        fun toColumn(x: Double): Int {
            return (round(x).toInt().clamp(startX, endX) - startX) / gap
        }

        fun toRow(y: Double): Int {
            return (round(y).toInt().clamp(startY, endY) - startY) / gap
        }

        fun toIndex(pos: Vector2): Int {
            return toRow(pos.y).clamp(0, nrRows - 1) * nrColumns + toColumn(pos.x).clamp(0, nrColumns-1)
        }

        fun toPos(row: Int, column: Int): Vector2 {
            return Vector2((startX + gap * column).toDouble(), (startY + gap * row).toDouble())
        }

        for (row in 0 until nrRows){
            for (column in 0 until nrColumns){
                grid.add(GridCell(toPos(row, column), row, column))
            }
        }

//        extend(ScreenRecorder()){
//            maximumDuration = duration
//            profile = GIFProfile()
//            frameRate = 4
//        }

        extend {
            fun field(gridCell: GridCell): Vector2 {
//            return Vector2(1.0, -1.0)
//                val angle = (gridCell.row / (nrRows - 1.0))*PI + seconds/duration * 2*PI
                val angle = simplex(0, gridCell.row*0.1, gridCell.column*0.1, cos(seconds/duration), sin(seconds/duration)) * PI
                return Vector2(cos(angle), sin(angle))
            }

            val arrows = grid.map { cell -> LineSegment(cell.pos, cell.pos + field(cell)*10.0) }

            fun makeCurve(start: Vector2, grid: List<GridCell>, nrSteps: Int): ShapeContour {
                var pos = start

                return contour {
                    moveTo(pos)
                    for (i in 0 until nrSteps) {
                        pos += field(grid[toIndex(pos)])
                        lineTo(pos)
                    }
                }
            }

//        val curves = List(40) { makeCurve(Vector2(startX + (it-10)*30.0 + 0.0, 200.0), grid, 550) }
            val rect = Rectangle(startX*1.0, startY*1.0, (endX-startX).toDouble(), (endY-startY).toDouble())
//            val curves = List(10000) { rect.randomPoint() }.map {
//                makeCurve(it, grid, gaussian(10.0, 15.0).toInt().clamp(5, 50))
//            }
//            val curves = emptyList()
//
//            drawer.apply {
//                clear(ColorRGBa.WHITE)
//                strokeWeight = 0.5
//                stroke = ColorRGBa.BLACK.opacify(0.2)
////                lineSegments(arrows)
//                contours(curves)
////                circles(arrows.map {it.start}, 5.0)
//            }
        }
    }
}