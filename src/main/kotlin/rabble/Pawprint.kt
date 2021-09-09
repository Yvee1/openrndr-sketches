package current

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.svg.loadSVG
import inverse_kinematics.*
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.DoubleParameter

fun Skeleton.draw(drawer: Drawer) {
    root.draw(drawer)
}

private fun Joint.draw(drawer: Drawer) {
    drawer.circle(position, 4.0)
    drawer.text(id, position + Vector2.ONE * 5.0)
    parentJoint?.let { parentJoint ->
        // Don't draw line if it has no parent
        drawer.lineSegment(parentJoint.position, position)
    }
    attachedJoints.forEach { it.draw(drawer) }
}

fun main() = application {
    configure {
        width = World.WIDTH.toInt()
        height = World.HEIGHT.toInt()
    }

    program {
        val font = org.openrndr.draw.loadFont("data/fonts/default.otf", 12.0)
        val paw = loadSVG("data/images/paw-fixed.svg")
        paw.findShapes().map { it.stroke = ColorRGBa.BLACK }

        val settings = Settings()
        val gui = GUI()
        gui.add(settings)

        extend(gui)
        extend {
            drawer.isolated {
                clear(ColorRGBa.WHITE)
//                translate(width/2.0, height/2.0)
//                composition(paw)
            }

            World.update(mouse.position, settings)
            drawer.fontMap = font
            drawer.stroke = ColorRGBa.BLACK
            drawer.fill = ColorRGBa.BLACK
//            World.gecko.skeleton.draw(drawer)

            fun drawFrontFoot(foot: Joint){
                drawer.isolated {
                    translate(foot.position)
                    rotate((foot.parentJoint!!.position - foot.position).angle().toDegrees() - 90.0)
                    scale(0.5)
                    translate(-paw.bounds.dimensions/2.0)
                    composition(paw)
                    // scale(1.0, -1.0) ???
                }
            }

            fun drawBackFoot(foot: Joint){
                drawer.isolated {
                    translate(foot.position)
                    rotate((foot.parentJoint!!.position - foot.position).angle().toDegrees() + 90.0)
                    scale(0.5)
                    translate(-paw.bounds.dimensions/2.0)
                    composition(paw)
                    // scale(1.0, -1.0) ???
                }
            }

//            drawBackFoot(World.gecko.backLeftFoot)
//            drawBackFoot(World.gecko.backRightFoot)
//            drawFrontFoot(World.gecko.frontLeftFoot)
//            drawFrontFoot(World.gecko.frontRightFoot)
            drawFrontFoot(World.gecko.frontFootDown)
            drawBackFoot(World.gecko.backFootDown)
        }
    }
}

object World {
    const val WIDTH = 1600.0
    const val HEIGHT = 900.0

    val gecko = Gecko.spawnGecko()

    fun update(objective: Vector2) {
        gecko.update(objective)
    }

    fun update(objective: Vector2, settings: Settings){
        gecko.update(objective, settings)
    }
}

class Settings {
    @DoubleParameter("Front foot target angle", 0.0, 180.0)
    var frontFootTargetAngle = 35.0
    @DoubleParameter("Front foot target separation", 0.0, 180.0)
    var frontFootTargetSeparation = 70.0

    //                                                 140.0
    @DoubleParameter("Back foot target angle", 0.0, 180.0)
    var backFootTargetAngle = 125.0
    @DoubleParameter("Back foot target separation", 0.0, 180.0)
    var backFootTargetSeparation = 35.0

    //                                                100.0
    // Maximum head turn per time step
    @DoubleParameter("Max turn", 0.0, 10.0)
    var maxTurn = 2.0

    @DoubleParameter("Min speed", 0.0, 10.0)
    var minSpeed = 0.0
    @DoubleParameter("Max speed", 0.0, 10.0)
    var maxSpeed = 0.5

    @DoubleParameter("Drag factor", 0.0, 4.0)
    var dragFactor = 0.2

    @DoubleParameter("Objective chasing factor", 0.0, 1.0)
    var objectiveChasingFactor = 0.01
    @DoubleParameter("Objective chasing radius", 0.0, 10.0)
    var objectiveReachedRadius = 5.0
}

class Gecko(spawnPosition: Vector2, var velocity: Vector2) {
    var settings = Settings()

    companion object {
        fun spawnGecko(): Gecko {
            return Gecko(Vector2(World.WIDTH / 2, World.HEIGHT / 2), -Vector2.UNIT_X*0.1)
        }
    }

    val skeleton: Skeleton
    val forces: MutableList<Vector2> = mutableListOf()
    var feetTargets: Map<Joint, Vector2> = mapOf()

    private val frontHip: Joint
    val frontLeftFoot: Joint
    val frontRightFoot: Joint
    private val backHip: Joint
    val backLeftFoot: Joint
    val backRightFoot: Joint

    var frontFootDown: Joint
    var backFootDown: Joint

    init {
        val head = Joint(spawnPosition, "head")

        frontHip = Joint(head.position + Vector2.UNIT_X * 30.0, "spine1")
//        val spine2 = Joint(frontHip.position + Vector2.UNIT_X * 30.0, "spine2")
//        val spine3 = Joint(spine2.position + Vector2.UNIT_X * 30.0, "spine3")
        val spine4 = Joint(frontHip.position + Vector2.UNIT_X * 30.0, "spine4")
        backHip = Joint(spine4.position + Vector2.UNIT_X * 30.0, "spine5")
        val spine6 = Joint(backHip.position + Vector2.UNIT_X * 30.0, "spine6")
        val spine7 = Joint(spine6.position + Vector2.UNIT_X * 30.0, "spine7")

        val frontLeftKnee = Joint(frontHip.position + Vector2.UNIT_Y * 20.0, "frontLeftKnee")
        frontLeftFoot = Joint(frontLeftKnee.position + Vector2.UNIT_Y * 30.0, "frontLeftFoot")
        val frontRightKnee = Joint(frontHip.position - Vector2.UNIT_Y * 20.0, "frontRightKnee")
        frontRightFoot = Joint(frontRightKnee.position - Vector2.UNIT_Y * 30.0, "frontRightFoot")

        val backLeftKnee = Joint(backHip.position + Vector2.UNIT_Y * 25.0, "backLeftKnee")
        backLeftFoot = Joint(backLeftKnee.position + Vector2.UNIT_Y * 35.0, "backLeftFoot")
        val backRightKnee = Joint(backHip.position - Vector2.UNIT_Y * 25.0, "backRightKnee")
        backRightFoot = Joint(backRightKnee.position - Vector2.UNIT_Y * 35.0, "backRightFoot")

        head.attachJoint(frontHip)

        frontHip.attachJoint(spine4, Pair(150.0.toRadians(), 210.0.toRadians()))
//        spine2.attachJoint(spine3, Pair(150.0.toRadians(), 210.0.toRadians()))
//        spine3.attachJoint(spine4, Pair(150.0.toRadians(), 210.0.toRadians()))
        spine4.attachJoint(backHip, Pair(150.0.toRadians(), 210.0.toRadians()))
        backHip.attachJoint(spine6, Pair(150.0.toRadians(), 210.0.toRadians()))
        spine6.attachJoint(spine7, Pair(150.0.toRadians(), 210.0.toRadians()))

        frontHip.attachJoint(frontLeftKnee, Pair(190.0.toRadians(), 345.0.toRadians()))
        frontLeftKnee.attachJoint(frontLeftFoot, Pair(190.0.toRadians(), 345.0.toRadians()))
        frontHip.attachJoint(frontRightKnee, Pair(15.0.toRadians(), 170.0.toRadians()))
        frontRightKnee.attachJoint(frontRightFoot, Pair(15.0.toRadians(), 170.0.toRadians()))

        backHip.attachJoint(backLeftKnee, Pair(190.0.toRadians(), 345.0.toRadians()))
        backLeftKnee.attachJoint(backLeftFoot, Pair(10.0.toRadians(), 165.0.toRadians()))
        backHip.attachJoint(backRightKnee, Pair(15.0.toRadians(), 170.0.toRadians()))
        backRightKnee.attachJoint(backRightFoot, Pair(195.0.toRadians(), 350.0.toRadians()))

        skeleton = Skeleton(head)

        frontFootDown = frontLeftFoot
        backFootDown = backRightFoot
    }

    fun update(objective: Vector2) {
        calculateVelocity(objective)
        move()
    }

    fun update(objective: Vector2, newSettings: Settings) {
        settings = newSettings
        update(objective)
    }

    private fun calculateVelocity(objective: Vector2) {
        // Calculate forces
        forces.clear()
        forces.add(dragForce())
        forces.add(objectiveChasingForce(objective))

        // Calculate updated velocity
        var newVelocity = velocity.copy()
        for (force in forces) {
            newVelocity += force
        }
        velocity = newVelocity
            .clampAbsoluteAngleDifference(velocity, settings.maxTurn.toRadians())
            .clampLength(settings.minSpeed, settings.maxSpeed)
    }

    private fun dragForce(): Vector2 {
        return -velocity * settings.dragFactor
    }

    private fun objectiveChasingForce(objective: Vector2): Vector2 {
        return if ((objective - skeleton.root.position).length < settings.objectiveReachedRadius) {
            Vector2.ZERO
        } else {
            (objective - skeleton.root.position) * settings.objectiveChasingFactor
        }
    }

    private fun move() {
        skeleton.root.position += velocity
        feetTargets = calculateFeetTargets(velocity)
        skeleton.solve(feetTargets)

        if (frontFootDown == frontLeftFoot && feetTargets[frontLeftFoot] != frontLeftFoot.position) {
            frontFootDown = frontRightFoot
        } else if (frontFootDown == frontRightFoot && feetTargets[frontRightFoot] != frontRightFoot.position) {
            frontFootDown = frontLeftFoot
        }

        if (backFootDown == backLeftFoot && feetTargets[backLeftFoot] != backLeftFoot.position) {
            backFootDown = backRightFoot
        } else if (backFootDown == backRightFoot && feetTargets[backRightFoot] != backRightFoot.position) {
            backFootDown = backLeftFoot
        }
    }

    private fun calculateFeetTargets(velocity: Vector2): Map<Joint, Vector2> {
        val frontLeftFootTarget = if (frontFootDown == frontLeftFoot) {
            frontLeftFoot.position
        } else {
            val stepTarget = frontHip.position + Vector2.unitWithRadiansAngle(
                (frontHip.parentJoint!!.position - frontHip.position).angle() - settings.frontFootTargetAngle.toRadians()
            ) * settings.frontFootTargetSeparation

            if (frontLeftFoot.position.distanceTo(stepTarget) < velocity.length * 3.0) {
                stepTarget
            } else {
                frontLeftFoot.position + (stepTarget - frontLeftFoot.position).setLength(velocity.length * 3.0)
            }
        }

        val frontRightFootTarget = if (frontFootDown == frontRightFoot) {
            frontRightFoot.position
        } else {
            val stepTarget = frontHip.position + Vector2.unitWithRadiansAngle(
                (frontHip.parentJoint!!.position - frontHip.position).angle() + settings.frontFootTargetAngle.toRadians()
            ) * settings.frontFootTargetSeparation

            if (frontRightFoot.position.distanceTo(stepTarget) < velocity.length * 3.0) {
                stepTarget
            } else {
                frontRightFoot.position + (stepTarget - frontRightFoot.position).setLength(velocity.length * 3.0)
            }
        }

        val backLeftFootTarget = if (backFootDown == backLeftFoot) {
            backLeftFoot.position
        } else {
            val stepTarget = backHip.position + Vector2.unitWithRadiansAngle(
                (backHip.parentJoint!!.position - backHip.position).angle() - settings.backFootTargetAngle.toRadians()
            ) * settings.backFootTargetSeparation

            if (backLeftFoot.position.distanceTo(stepTarget) < velocity.length * 3.0) {
                stepTarget
            } else {
                backLeftFoot.position + (stepTarget - backLeftFoot.position).setLength(velocity.length * 3.0)
            }
        }

        val backRightFootTarget = if (backFootDown == backRightFoot) {
            backRightFoot.position
        } else {
            val stepTarget = backHip.position + Vector2.unitWithRadiansAngle(
                (backHip.parentJoint!!.position - backHip.position).angle() + settings.backFootTargetAngle.toRadians()
            ) * settings.backFootTargetSeparation

            if (backRightFoot.position.distanceTo(stepTarget) < velocity.length * 3.0) {
                stepTarget
            } else {
                backRightFoot.position + (stepTarget - backRightFoot.position).setLength(velocity.length * 3.0)
            }
        }

        return mapOf(
            Pair(frontLeftFoot, frontLeftFootTarget),
            Pair(frontRightFoot, frontRightFootTarget),
            Pair(backLeftFoot, backLeftFootTarget),
            Pair(backRightFoot, backRightFootTarget)
        )
    }
}