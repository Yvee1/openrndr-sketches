package useful

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import space.kscience.kmath.complex.*
import space.kscience.kmath.complex.ComplexField.div
import space.kscience.kmath.complex.ComplexField.minus
import space.kscience.kmath.complex.ComplexField.plus
import space.kscience.kmath.complex.ComplexField.times
import space.kscience.kmath.linear.Matrix
import space.kscience.kmath.operations.*
import useful.ExtendedComplexField.div
import useful.ExtendedComplexField.plus
import useful.ExtendedComplexField.unaryMinus
import kotlin.math.atan2
import kotlin.math.pow

typealias CMatrix = Matrix<Complex>
typealias ECMatrix = Matrix<EComplex>

sealed class EComplex {
}
object inf : EComplex(){
    public override fun toString(): String = "inf"
}
class EC(val z: Complex) : EComplex(){
    public override fun toString(): String = z.toString()
}

public val EComplex.reciprocal: EComplex
    get() = when (this) {
        is inf -> EC(Complex(0))
        is EC  -> EC(z.reciprocal)
    }

public val EComplex.conjugate: EComplex
    get() = when (this) {
        is inf -> inf
        is EC  -> EC(z.conjugate)
    }

public val EComplex.r: Double
    get() = when (this) {
        is inf -> Double.NaN
        is EC  -> z.r
    }

//object ExtendedComplexField : ExtendedField<EComplex>, Norm<EComplex, EComplex>, NumbersAddOperations<EComplex>,
//    ScaleOperations<EComplex> {
object ExtendedComplexField : Field<EComplex>, NumbersAddOperations<EComplex>, ScaleOperations<EComplex> {
    public override val zero: EComplex = 0.0.toEComplex()
    public override val one: EComplex = 1.0.toEComplex()

    /**
     * The imaginary unit.
     */
    public val i: EComplex by lazy { EC(Complex(0.0, 1.0)) }

    override fun EComplex.unaryMinus(): EComplex = when(this){
        is EC -> EC( ComplexField { -z })
        is inf -> inf
    }

    override fun number(value: Number): EComplex = EC(Complex(value.toDouble(), 0.0))

    override fun scale(a: EComplex, value: Double): EComplex = when(a){
        is EC -> EC( ComplexField { scale(a.z, value) })
        is inf -> if (value == 0.0) throw ArithmeticException("0 * inf") else inf
    }

    public override fun add(a: EComplex, b: EComplex): EComplex = when(a){
        is EC -> when(b){
            is EC -> EC( ComplexField { add(a.z, b.z) })
            is inf -> inf
        }
        is inf -> when (b){
            is EC -> inf
            is inf -> throw ArithmeticException("inf + inf")
        }
    }
//    public override fun multiply(a: Complex, k: Number): Complex = Complex(a.re * k.toDouble(), a.im * k.toDouble())

    public override fun multiply(a: EComplex, b: EComplex): EComplex = when(a){
        is EC -> when(b){
            is EC -> EC( ComplexField { multiply(a.z, b.z) })
            is inf -> inf
        }
        is inf -> when (b){
            is EC -> inf
            is inf -> throw ArithmeticException("inf * inf")
        }
    }

    public override fun divide(a: EComplex, b: EComplex): EComplex = when(a) {
        is inf -> when(b){
            is inf -> throw ArithmeticException("inf / inf")
            is EC  -> inf
        }
        is EC -> when(b) {
            is inf -> EC(Complex(0))
            is EC -> when {
//                a.z == Complex(0) && b.z == Complex(0) -> throw ArithmeticException("0 / 0")
                a.z.r2 == 0.0 && b.z.r2 == 0.0 -> throw ArithmeticException("0 / 0")
                b.z.r2 == 0.0 -> inf
                else -> EC(ComplexField { divide(a.z, b.z) })
            }
        }
//
//        a is EC && b is EC && kotlin.math.abs(b.z.im) < kotlin.math.abs(b.z.re) -> {
//            val wr = b.z.im / b.z.re
//            val wd = b.z.re + wr * b.z.im
//
//            if (wd.isNaN())
//                throw ArithmeticException("Division by infinity")
//            else if (wd == 0.0)
//                inf
//            else
//                EC(Complex((a.z.re + a.z.im * wr) / wd, (a.z.im - a.z.re * wr) / wd))
//        }
//
//        a is EC && b is EC && b.z.im == 0.0 -> inf
//
//        a is EC && b is EC && kotlin.math.abs(b.z.im) > kotlin.math.abs(b.z.re) -> {
//            val wr = b.z.re / b.z.im
//            val wd = b.z.im + wr * b.z.re
//
//            if (wd.isNaN())
//                throw ArithmeticException("Division by infinity")
//            else if (wd == 0.0)
//                inf
//            else
//                EC(Complex((a.z.re * wr + a.z.im) / wd, (a.z.im * wr - a.z.re) / wd))
//        }
    }

    override operator fun EComplex.div(k: Number): EComplex = when(this) {
        is inf -> if (k == 0.0) throw ArithmeticException("inf / 0") else inf
        is EC  -> {
            if (z == Complex(0) && k == 0.0)
                throw ArithmeticException("0 / 0")
            else if (k == 0.0)
                inf
            else EC(z.div(k))
        }
    }
//
//    public override fun sin(arg: EComplex): EComplex = i * (exp(-i * arg) - exp(i * arg)) / 2.0
//    public override fun cos(arg: Complex): Complex = (exp(-i * arg) + exp(i * arg)) / 2.0
//
//    public override fun tan(arg: Complex): Complex {
//        val e1 = exp(-i * arg)
//        val e2 = exp(i * arg)
//        return i * (e1 - e2) / (e1 + e2)
//    }
//
//    public override fun asin(arg: Complex): Complex = -i * ln(ComplexField.sqrt(1 - (arg * arg)) + i * arg)
//    public override fun acos(arg: Complex): Complex = PI_DIV_2 + i * ln(ComplexField.sqrt(1 - (arg * arg)) + i * arg)
//
//    public override fun atan(arg: Complex): Complex {
//        val iArg = i * arg
//        return i * (ln(1 - iArg) - ln(1 + iArg)) / 2
//    }
//
//    public override fun power(arg: Complex, pow: Number): Complex = if (arg.im == 0.0)
//        arg.re.pow(pow.toDouble()).toComplex()
//    else
//        exp(pow * ln(arg))
//
//    public override fun exp(arg: Complex): Complex = kotlin.math.exp(arg.re) * (kotlin.math.cos(arg.im) + i * kotlin.math.sin(
//        arg.im
//    ))
//
//    public override fun ln(arg: Complex): Complex = kotlin.math.ln(arg.r) + i * atan2(arg.im, arg.re)

    /**
     * Adds complex number to real one.
     *
     * @receiver the augend.
     * @param c the addend.
     * @return the sum.
     */
    public operator fun Double.plus(c: EComplex): EComplex = add(EC(this.toComplex()), c)

    /**
     * Subtracts complex number from real one.
     *
     * @receiver the minuend.
     * @param c the subtrahend.
     * @return the difference.
     */
    public operator fun Double.minus(c: EComplex): EComplex = add(EC(this.toComplex()), -c)

    /**
     * Adds real number to complex one.
     *
     * @receiver the augend.
     * @param d the addend.
     * @return the sum.
     */
    public operator fun EComplex.plus(d: Double): EComplex = d + this

    /**
     * Subtracts real number from complex one.
     *
     * @receiver the minuend.
     * @param d the subtrahend.
     * @return the difference.
     */
    public operator fun EComplex.minus(d: Double): EComplex = add(this, -EC(d.toComplex()))

    /**
     * Multiplies real number by complex one.
     *
     * @receiver the multiplier.
     * @param c the multiplicand.
     * @receiver the product.
     */
    public operator fun Double.times(c: EComplex): EComplex = multiply(EC(Complex(this)), c)

//    public override fun norm(arg: EComplex): EComplex = ComplexField.sqrt(arg.conjugate * arg)

    public override fun bindSymbolOrNull(value: String): EComplex? = if (value == "i") i else null
}

public fun Number.toEComplex(): EComplex = EC(this.toComplex())


fun Complex.toVector2() = Vector2(re, im)
fun Complex.toVector3() = Vector3(re, 0.0, im)
public val Complex.r2: Double
    get() = re * re + im * im
fun Vector2.toComplex() = Complex(x, y)

fun EComplex.toVector2() = when(this) {
    is inf -> Vector2(1000.0, 1000.0)
    is EC  -> z.toVector2()
}
fun Vector2.toEComplex() = EC(Complex(x, y))

fun complexToSphere(z: Complex) = Vector3(z.re, z.re * z.re + z.im * z.im, z.im) / (z.re * z.re + z.im * z.im + 1)
fun sphereToComplex(v: Vector3) = Complex(v.x / (1 - v.y), v.z / (1 - v.y))

//fun mobiusOnCirc(T: CMatrix, C: Circle): Circle {
//    println(T[1, 1]/T[1, 0])
//    println(C.center.toComplex())
//    println((T[1, 1]/T[1, 0] + C.center.toComplex()).conjugate)
//    val z = ComplexField { (C.center.toComplex() - (C.radius * C.radius)) / (T[1, 1]/T[1, 0] + C.center.toComplex()).conjugate }
//    val center = mobiusOnPoint(T, z)
//    val radius = ComplexField { center - mobiusOnPoint(T, ComplexField { C.center.toComplex() + C.radius }) }.r
//    return Circle(center.toVector2(), radius)
//}
//
//fun mobiusOnPoint(T: CMatrix, z: Complex): Complex {
//    return ComplexField { (T[0, 0] * z + T[0, 1]) / (T[1, 0] * z + T[1, 1]) }
//}
//
//fun mobiusOnCirc(T: ECMatrix, C: Circle): Circle {
//    val z = ExtendedComplexField { (C.center.toEComplex() - (C.radius * C.radius)) / (T[1, 1]/T[1, 0] + C.center.toEComplex()).conjugate }
//    val center = mobiusOnPoint(T, z)
//    val radius = ExtendedComplexField { center - mobiusOnPoint(T, ExtendedComplexField { C.center.toEComplex() + C.radius }) }
////    println(C.center.toEComplex() + C.radius)
////    println(T)
////    println(mobiusOnPoint(T, ExtendedComplexField { C.center.toEComplex() + C.radius }))
//
//    return Circle(center.toVector2(), radius.r)
//}
fun mobiusOnCirc(T: ECMatrix, C: Circle): Circle {
    val pts = C.contour.equidistantPositions(3).map { mobiusOnPoint(T, it.toEComplex()).toVector2() }
    return Circle.fromPoints(pts[0], pts[1], pts[2])
}

fun mobiusOnPoint(T: ECMatrix, z: EComplex): EComplex {
    return ExtendedComplexField { (T[0, 0] * z + T[0, 1]) / (T[1, 0] * z + T[1, 1]) }
}