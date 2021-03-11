import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.noise.uniforms
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extras.color.presets.CYAN
import org.openrndr.math.CatmullRomChain2
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.pow

class Attractor(val pos: Vector2, val attractionDistance: Double, val killDistance: Double){
    var reached = false
    val range = Circle(pos, attractionDistance)
    val attracts = mutableListOf<Node>()

    companion object {
        fun draw(drawer: Drawer, attractors: List<Attractor>){
            drawer.apply {
                fill = ColorRGBa.CYAN
                stroke = ColorRGBa.BLACK
                strokeWeight = 1.0
                circles(attractors.map{it.pos}, 5.0)

//                strokeWeight = 3.0
//                fill = ColorRGBa.BLACK.opacify(0.1)
//                circles(attractors.map{it.range})
            }
        }
    }
}

class Node(val parent: Node?, val pos: Vector2, var dir: Vector2, val segmentLength: Double){
    val children = mutableListOf<Node>()
    val attractedBy = mutableListOf<Attractor>()

    fun spawn(): Node {
        val child = Node(this, pos + dir * segmentLength, dir, segmentLength * 0.98)
        children.add(child)
        return child
    }

    fun grow(): Node? {
        return if (attractedBy.size > 0 && segmentLength > 1) {
            dir = attractedBy.fold(dir) { sum, attractor -> sum + (attractor.pos - this.pos).normalized }.normalized
            spawn()
        } else {
            null
        }
    }

    companion object {
        fun draw(drawer: Drawer, nodes: List<Node>){
            drawer.apply {
                fill = ColorRGBa.BLACK
//                stroke = ColorRGBa.BLACK
                stroke = null
                strokeWeight = 0.0
                circles(nodes.map {it.pos}, nodes.map { it.segmentLength/2.0 })
            }
        }
    }
}

class Network(val pos: Vector2, val dir: Vector2, val bounds: Rectangle, val attractors: List<Attractor>, val segmentLength: Double) {
    val root = Node(null, pos, dir, segmentLength)
    val nodeQuadTree = QuadTree<Node>(bounds, 4) { node -> node.pos }
    var grown = false
    var current = root

    init {
        nodeQuadTree.insert(root)
    }

    fun closeEnough(): Boolean {
        var found = false
        attractors.forEach {
            val attractedNodes = nodeQuadTree.query(it.range)
            if (attractedNodes.isNotEmpty()){
                found = true
            }
        }

        return found
    }

    fun growBase(){
        if (grown){
            return
        }

        if (!closeEnough()) {
            val newBranch = current.spawn()
            nodeQuadTree.insert(newBranch)
            current = newBranch
        } else {
            // Temporarily
            grown = true
        }
    }

    fun grow(additionalAttractors: List<Attractor>){
        nodeQuadTree.forEach { it.attractedBy.clear() }

        additionalAttractors.forEach { attractor ->
            if (!attractor.reached) {
                attractor.attracts.clear()
                val attractedNodes = nodeQuadTree.query(attractor.range)
                val iterator = attractedNodes.iterator()
                if (iterator.hasNext()){
                    var closest = iterator.next()
                    var closestDist = closest.pos.squaredDistanceTo(attractor.pos)
                    if (iterator.hasNext()) {
                        do {
                            val node = iterator.next()
                            val dist = node.pos.squaredDistanceTo(attractor.pos)
                            if (dist < closestDist) {
                                closest = node
                                closestDist = dist
                            }
                        } while (iterator.hasNext())
                    }
                    if (closestDist <= attractor.killDistance.pow(2)) {
                        attractor.reached = true
                    } else {
                        closest.attractedBy.add(attractor)
                        attractor.attracts.add(closest)
                    }
                }
            }
        }

        attractors.forEach { attractor ->
            if (!attractor.reached) {
                attractor.attracts.clear()
                val attractedNodes = nodeQuadTree.query(attractor.range)
                val iterator = attractedNodes.iterator()
                if (iterator.hasNext()){
                    var closest = iterator.next()
                    var closestDist = closest.pos.squaredDistanceTo(attractor.pos)
                    if (iterator.hasNext()) {
                        do {
                            val node = iterator.next()
                            val dist = node.pos.squaredDistanceTo(attractor.pos)
                            if (dist < closestDist) {
                                closest = node
                                closestDist = dist
                            }
                        } while (iterator.hasNext())
                    }
                    if (closestDist <= attractor.killDistance.pow(2)) {
                        attractor.reached = true
                    } else {
                        closest.attractedBy.add(attractor)
                        attractor.attracts.add(closest)
                    }
                }
            }
        }


        val newNodes = mutableListOf<Node>()
        nodeQuadTree.forEach { node ->
            node.grow()?.let { child ->
                newNodes.add(child)
            }
        }
        newNodes.forEach { nodeQuadTree.insert(it) }
    }

    fun draw(drawer: Drawer){
        drawer.apply {
//            Attractor.draw(this, attractors)
            Node.draw(this, nodeQuadTree.getAllElements())

//            fill = null
//            stroke = ColorRGBa.BLACK
//            strokeWeight = 0.5
//            nodeQuadTree.draw(drawer)
        }
    }
}

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        val killDistance = 10.0
        val attractors = Vector2.uniforms(1000, Rectangle(Vector2.ZERO, width*1.0, height*0.6)).map {
            Attractor(it, 100.0, killDistance)
        }

        val boundPadding = 100.0
        val bounds = Rectangle(-boundPadding, -boundPadding, width + boundPadding*2, height + boundPadding*2)
        val network = Network(Vector2(width/2.0+0.0, height*1.0), Vector2(0.0, -1.0), bounds, attractors, 20.0)

        extend {
            drawer.clear(ColorRGBa.WHITE)
            network.draw(drawer)
            network.growBase()
//            network.grow(listOf(Attractor(mouse.position, 100.0, killDistance)))
            network.grow(listOf())
        }
    }
}

fun List<Vector2>.bounds(): Rectangle {
    var minX = this[0].x
    var maxX = minX
    var minY = this[0].y
    var maxY = minY

    this.forEach {
        if (it.x < minX){
            minX = it.x
        }
        if (it.x > maxX){
            maxX = it.x
        }
        if (it.y < minY){
            minY = it.y
        }
        if (it.y > maxY){
            maxY = it.y
        }
    }

    return Rectangle(minX, minY, maxX - minX, maxY - minY)
}