import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

fun vector2Mapper(v: Vector2) = v

class QuadTree<T>(val boundary: Rectangle, val capacity: Int, val mapper: (T) -> Vector2){
    val elements = mutableListOf<T>()
    var subdivided = false

    var northWest: QuadTree<T>? = null
    var northEast: QuadTree<T>? = null
    var southWest: QuadTree<T>? = null
    var southEast: QuadTree<T>? = null

    fun insert(element: T): Boolean {
        val point = mapper(element)
        if (!boundary.contains(point)){
            return false
        }
        else if (elements.size < capacity || boundary.width < 1e-7 || boundary.height < 1e-7){
            elements.add(element)
            return true
        }
        else {
            if (!subdivided) subdivide()
            if (northWest!!.insert(element)) return true
            if (northEast!!.insert(element)) return true
            if (southWest!!.insert(element)) return true
            if (southEast!!.insert(element)) return true

            throw IllegalArgumentException("Point $point was not inside bounds $boundary")
        }
    }

    fun subdivide(){
        val smaller = Rectangle(boundary.x, boundary.y, boundary.width/2, boundary.height/2)
        northWest = QuadTree(smaller, capacity, mapper)
        northEast = QuadTree(smaller.moved(Vector2(boundary.width/2, 0.0)), capacity, mapper)
        southWest = QuadTree(smaller.moved(Vector2(0.0, boundary.height/2)), capacity, mapper)
        southEast = QuadTree(smaller.moved(Vector2(boundary.width/2, boundary.height/2)), capacity, mapper)
        subdivided = true
    }

    interface Queryable {
        fun intersects(rect: Rectangle): Boolean
        fun containsRect(rect: Rectangle): Boolean
        fun containsPt(pt: Vector2): Boolean
    }

    private fun Rectangle.toGenericQueryable() = object : Queryable {
        override fun intersects(other: Rectangle): Boolean {
            return !(other.x - other.width > x + width ||
                    other.x + other.width < x - width ||
                    other.y - other.height > y + height ||
                    other.y + other.height < y - height)
        }

        override fun containsRect(other: Rectangle): Boolean {
            return (corner.x <= other.corner.x && corner.y <= other.corner.y
                    && corner.x + width >= other.corner.x + other.width
                    && corner.y + height >= other.corner.y + other.height)
        }

        override fun containsPt(pt: Vector2): Boolean = contains(pt)
    }

    private fun Circle.toGenericQueryable() = object : Queryable {
        override fun intersects(rect: Rectangle): Boolean {
            val xDist = abs(rect.x - center.x)
            val yDist = abs(rect.y - center.y)

            if (xDist > radius + rect.width || yDist > radius + rect.height) return false
            if (xDist <= rect.width || yDist <= rect.height) return true

            return (xDist - rect.width).pow(2) + (yDist - rect.height).pow(2) <= radius.pow(2)
        }

        override fun containsRect(rect: Rectangle): Boolean {
            return Rectangle.fromCenter(center, radius / sqrt(2.0)).toGenericQueryable().containsRect(rect)
        }

        override fun containsPt(pt: Vector2): Boolean = contains(pt)
    }

    fun query(rect: Rectangle): List<T> {
        return query(rect.toGenericQueryable())
    }

    fun query(circle: Circle): List<T> {
        return query(circle.toGenericQueryable())
    }

    private fun query(range: Queryable): List<T> {
        fun queryStep(qt: QuadTree<T>, found: MutableList<T>): MutableList<T> {
            // If there is no intersection, no new points can be found
            if (!range.intersects(qt.boundary)) {
                return found
            } else if (range.containsRect(qt.boundary)) {
                // Add points from this node that are contained in the range
                found.addAll(qt.elements)
            } else {
                for (p in qt.elements) {
                    if (range.containsPt(qt.mapper(p))) {
                        found.add(p)
                    }
                }
            }

            // Add points from children that are contained in the range
            if (qt.subdivided) {
                queryStep(qt.northWest!!, found)
                queryStep(qt.northEast!!, found)
                queryStep(qt.southWest!!, found)
                queryStep(qt.southEast!!, found)
            }

            return found
        }

        return queryStep(this, mutableListOf()).toList()
    }

    fun forEach(action: (T) -> Unit) {
        elements.forEach(action)
        forEachChild { it.forEach(action) }
    }

    private fun forEachChild(action: (QuadTree<T>) -> Unit){
        if (subdivided){
            action(northWest!!)
            action(northEast!!)
            action(southWest!!)
            action(southEast!!)
        }
    }

    fun getAllElements(): List<T> {
        fun step(qt: QuadTree<T>, found: MutableList<T>): MutableList<T> {
            found.addAll(qt.elements)
            qt.forEachChild { step(it, found) }
            return found
        }

        return step(this, mutableListOf()).toList()
    }

    fun draw(drawer: Drawer) {
        drawer.rectangle(boundary)
        drawer.circles(elements.map(mapper), 5.0)
        northWest?.draw(drawer)
        northEast?.draw(drawer)
        southWest?.draw(drawer)
        southEast?.draw(drawer)
    }


    override fun toString(): String {
        return "(${boundary.corner.x}, ${boundary.corner.y}, ${boundary.width}, ${boundary.width}): " +
                "${elements.size}\n" +
                (northWest?.toString() ?: "") +
                (northEast?.toString() ?: "") +
                (southWest?.toString() ?: "") +
                (southEast?.toString() ?: "")
    }
}

fun main(){
    applicationSynchronous {
        configure {
            width = 800
            height = 800
        }

        oliveProgram {
            val bounds = Rectangle(0.0, 0.0, 800.0, 800.0)
            val qt = QuadTree(bounds, 1, ::vector2Mapper)
            for (i in 0 until 1000){
                qt.insert(Vector2.uniform(bounds))
            }

            fun <T> QuadTree<T>.draw(drawer: Drawer) {
                drawer.rectangle(boundary)
                drawer.circles(elements.map(mapper), 5.0)
                northWest?.draw(drawer)
                northEast?.draw(drawer)
                southWest?.draw(drawer)
                southEast?.draw(drawer)
            }

            extend {
//                val range = Rectangle.fromCenter(mouse.position, 100.0, 100.0)
                val range = Circle(mouse.position, 100.0)
                val pts = qt.query(range)

                drawer.apply {
                    clear(ColorRGBa.WHITE)
                    stroke = ColorRGBa.BLACK
                    strokeWeight = 0.5
                    fill = null
                    qt.draw(drawer)

                    stroke = ColorRGBa.GREEN
                    strokeWeight = 2.0
                    circle(range)
//                    rectangle(range)
                    circles(pts, 5.0)
                }
            }
        }
    }
}