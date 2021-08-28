package current

import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawQuality
import org.openrndr.extensions.Screenshots
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

fun main() = applicationSynchronous {
    program {
        extend(Screenshots())
        extend {
            val pt1 = Vector2(100.0, 100.0)
            val pt2 = Vector2(150.0, 150.0)
            val pt3 = Vector2(200.0, 100.0)
            val pts = listOf(pt1, pt2, pt3)

            drawer.apply {
                drawStyle.quality = DrawQuality.PERFORMANCE
                stroke = ColorRGBa.PINK
                lineLoop(pts)
                translate(200.0, 0.0)
                lineLoop(pts.map { Vector3(it.x, it.y, 0.0) })
            }
        }
    }
}