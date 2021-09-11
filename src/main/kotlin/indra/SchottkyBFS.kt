package indra

import org.openrndr.PresentationMode
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extras.color.presets.ORANGE
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import space.kscience.kmath.complex.ComplexField
import space.kscience.kmath.operations.*
import space.kscience.kmath.complex.*
import space.kscience.kmath.linear.LinearSpace
import space.kscience.kmath.linear.Matrix
import space.kscience.kmath.linear.invoke
import space.kscience.kmath.linear.matrix
import useful.*

//val cMatrixField = LinearSpace.auto(ComplexField)
val cMatrixField: LinearSpace<EComplex, ExtendedComplexField> = LinearSpace.auto(ExtendedComplexField)
fun <T : Any, A : Ring<T>> LinearSpace<T, A>.buildMatrix(rows: Int, columns: Int, values: List<T>): Matrix<T> =
    buildMatrix(rows, columns) { i, j -> values[i*columns + j] }

const val s = 0.1
const val t = 0.9

fun makeMatrix(a: Double, b: Double, c: Double, d: Double) = cMatrixField { cMatrixField.buildMatrix(2, 2, listOf(EC(Complex(a, 0)), EC(Complex(b, 0)), EC(Complex(c, 0)), EC(Complex(d, 0)))) * EC(Complex(s-t, 0)).reciprocal }

val a = makeMatrix(s+t, -2*s*t, -2.0, s+t)
val aInv = makeMatrix(s+t, 2*s*t, 2.0, s+t)
val b = makeMatrix(s+t, 2.0, 2*s*t, s+t)
val bInv = makeMatrix(s+t, -2.0, -2*s*t, s+t)

val gens = arrayOf(a, b, aInv, bInv)
val group = arrayOfNulls<ECMatrix>(1000000)
val circ = arrayOf(Circle(-(s+t)/2, 0.0, (t-s)/2), Circle((s+t)/2, 0.0, (t-s)/2), Circle(-(1/s + 1/t)/2, 0.0, (1/s - 1/t)/2), Circle((1/s + 1/t)/2, 0.0, (1/s - 1/t)/2))
val colors = listOf(ColorRGBa.RED, ColorRGBa.GREEN, ColorRGBa.BLUE, ColorRGBa.ORANGE)

const val levMax: Int = 8
val tag = arrayOfNulls<Int>(1000000)
val inv = listOf(2, 3, 0, 1)
val num = arrayOfNulls<Int>(levMax+1)

fun main() = application {
    configure {
        windowResizable = true
    }

    program {
        window.presentationMode = PresentationMode.MANUAL


        extend {
//            val nmouse = (mouse.position - Vector2(width/2.0, height/2.0))/70.0

//            print(nmouse)
//            print("\r")
//            println(mobiusOnCirc(bInv, circ[1]))
            drawer.translate(width/2.0, height/2.0)
            drawer.scale(70.0)

            for (i in 0 until 4) {
                drawer.fill = colors[i]
                drawer.stroke = null
                group[i] = gens[i]
                tag[i] = i
                drawer.circle(circ[i])
            }
            drawer.strokeWeight = 2.0/55.0
            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(s, 10.0, s, -10.0)
            drawer.lineSegment(t, 10.0, t, -10.0)
            drawer.lineSegment(1/s, 10.0, 1/s, -10.0)
            drawer.lineSegment(1/t, 10.0, 1/t, -10.0)

            num[0] = 0
            num[1] = 4

            for (lev in 1 until levMax){
                var iNew = num[lev]!!
                for (iOld in num[lev-1]!! until num[lev]!!){
                    for (j in 0 until 4) {
                        if (j == inv[tag[iOld]!!]) continue

//                        println(group[iOld]!!)
//                        println(gens[j]!!)

                        group[iNew] = cMatrixField {
                            group[iOld]!! dot gens[j]
                        }

//                        println(group[iNew])

                        tag[iNew] = j
                        iNew += 1
                    }
                }
                num[lev + 1] = iNew
            }

            for (i in 0 until num[levMax]!!){
                for (j in 0 until 4) {
                    if (j == inv[tag[i]!!]) continue
//                    println(group[i])
//                    println(circ[j])

                    try {
                        val newCirc = mobiusOnCirc(group[i]!!, circ[j])
                        drawer.apply {
                            stroke = null
                            fill = ColorRGBa(i / levMax.toDouble(), j / 4.0, 0.7, 0.5)
                            circle(newCirc)
                        }
                    } catch (e: ArithmeticException) {

                    }
                }
            }
        }
    }
}