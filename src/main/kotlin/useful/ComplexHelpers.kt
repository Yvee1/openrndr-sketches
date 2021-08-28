package useful

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import space.kscience.kmath.complex.Complex

fun Complex.toVector2() = Vector2(re, im)
fun Complex.toVector3() = Vector3(re, 0.0, im)
fun Complex.r2() = re * re + im * im
fun Vector2.toComplex() = Complex(x, y)

fun complexToSphere(z: Complex) = Vector3(z.re, z.re * z.re + z.im * z.im, z.im) / (z.re * z.re + z.im * z.im + 1)