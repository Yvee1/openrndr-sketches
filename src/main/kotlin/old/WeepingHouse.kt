import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.shadow.DropShadow
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extras.color.presets.*
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.shape.ClipMode
import org.openrndr.shape.CompositionDrawer
import org.openrndr.shape.Rectangle
import org.openrndr.shape.drawComposition
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

fun main() = applicationSynchronous {
    configure {
        width = 800
        height = 800
    }

    oliveProgram {
        class Brick(val pos: Vector2, val brickWidth: Double, val brickHeight: Double, var color: ColorRGBa){
            val wetColor: ColorRGBa by lazy { ColorRGBa.DODGER_BLUE.shade(gaussian(0.8, 0.1)) }
            var wet = 0.0
            val dryingSpeed = 0.02

            fun draw(drawer: Drawer) {
                drawer.apply {
                    fill = color.mix(wetColor, wet)
                    rectangle(pos, brickWidth, brickHeight)
                }
                wet = max(wet-dryingSpeed, 0.0)
            }

            fun draw(drawer: CompositionDrawer) {
                drawer.apply {
                    fill = color.mix(wetColor, wet)
                    contour(Rectangle(pos.x, pos.y, brickWidth, brickHeight).contour)
                }
                wet = max(wet-dryingSpeed, 0.0)
            }
        }
        val bw = 40.0
        val bh = 16.0

        val hw = width * 1.1
        val hh = height * 1.1
        val pos = Vector2(width/2.0, height*1.0)

        val bricks = mutableListOf<Brick>()
        val nw = (hw / bw).toInt()
        val nh = (hh / bh).toInt()
        for (j in 0 until nh){
            val off = gaussian(0.0, 1.0)
            for (i in 0 until nw){
                val c = ColorRGBa.GHOST_WHITE.shade(gaussian(0.9, 0.02))
                val x = i*bw + (j % 2)*bw*0.5
                val y = -j*bh
                if (x == 0.0 || x == (nw-1+0.5)*bw){
                    bricks.add(Brick(pos + Vector2(x + off - hw / 2.0 + if (x == 0.0) 0.5*bw else 0.0, y), bw*0.5, bh, c))
                } else {
                    bricks.add(Brick(pos + Vector2(x - hw / 2.0 + off, y), bw, bh, c))
                }
            }
        }

        fun getBrick(target: Vector2): Brick? {
            return bricks.minByOrNull { (it.pos + Vector2(it.brickWidth, it.brickHeight)/2.0 - target).squaredLength }
        }

        val frameSize = 13.0
        val window1 = Rectangle(width*0.2+0.5, height*0.1+0.5, 150.0, 200.0)
        val frame1 = window1.copy(corner=Vector2(window1.x - frameSize, window1.y - frameSize), width=window1.width+2*frameSize, height=window1.height+2*frameSize)
        val lid1 = window1.copy(height=50.0)
        val gap11 = window1.copy(corner=Vector2(window1.x, lid1.y+lid1.height+frameSize/2.0), height=window1.height-lid1.height)
        val gap12 = window1.copy(corner=Vector2(window1.x, window1.y), height=lid1.height-frameSize/2.0)
        val window2 = window1.copy(corner=Vector2(width-window1.x-window1.width, window1.y))
        val frame2 = window2.copy(corner=Vector2(window2.x - frameSize, window2.y - frameSize), width=window2.width+2*frameSize, height=window2.height+2*frameSize)
        val lid2 = window2.copy(height=50.0)
        val gap21 = window2.copy(corner=Vector2(window2.x, lid2.y+lid2.height+frameSize/2.0), height=window2.height-lid2.height)
        val gap22 = window2.copy(corner=Vector2(window2.x, window2.y), height=lid2.height-frameSize/2.0)

        val nBottomLid = 6
        val bottomLidColors1 = List(nBottomLid*2) {
            ColorRGBa.GHOST_WHITE.shade(gaussian(0.85, 0.02))
        }
        val bottomLidColors2 = List(nBottomLid*2) {
            ColorRGBa.GHOST_WHITE.shade(gaussian(1.0, 0.02))
        }
        val nBlinds = 50

        val blindColors1 = List(nBlinds) {
            ColorRGBa.WHITE.shade(gaussian(0.84, 0.03))
        }
        val blindColors2 = List(nBlinds) {
            ColorRGBa.WHITE.shade(gaussian(0.84, 0.03))
        }

        val tears1 = MutableList(15) { i ->
            frame1.corner + Vector2(frame1.width*Double.uniform(0.2, 0.8), frame1.height + i*35.0 + gaussian(50.0, 10.0))
        }
        val tears2 = MutableList(15) { i ->
            frame2.corner + Vector2(frame2.width*Double.uniform(0.2, 0.8), frame2.height + i*35.0 + gaussian(50.0, 10.0))
        }
        val tears = (tears1 + tears2).toMutableList()

        val tearSpeed = 2.0

        val effect1 = DropShadow()
        effect1.gain = 0.646
        effect1.window = 6
        effect1.xShift = 0.0
        effect1.yShift = -30.0

        val effect2 = DropShadow()
        effect2.gain = 0.845
        effect2.window = 4
        effect2.xShift = 2.795
        effect2.yShift = -5.4

        val effect3 = DropShadow()
        effect3.gain = 0.919
        effect3.window = 4
        effect3.xShift = 1.304
        effect3.yShift = -5.776

        val effect4 = DropShadow()
        effect4.gain = 0.85
        effect4.window = 3
        effect4.xShift = 3.7
        effect4.yShift = -7.2

        val gui = GUI()
        effect1.addTo(gui)
        effect2.addTo(gui)
        effect3.addTo(gui)
        effect4.addTo(gui)
        val settings = object : Animatable() {
            @ColorParameter("Inside window color")
            var color = ColorRGBa(0.554, 0.503, 0.7333)

            @DoubleParameter("Blinds1", 0.0, 1.0)
            var blind1Closed = 0.5

            @DoubleParameter("Blinds2", 0.0, 1.0)
            var blind2Closed = 0.6
        }

        gui.add(settings, "Settings")

        val c = compose {
            // Background
            layer {
                draw {
                    drawer.apply {
                        clear(settings.color)
                    }
                }
            }

            layer {
                draw {
                    drawer.apply {
                        fill = ColorRGBa.GHOST_WHITE
                        stroke = ColorRGBa.DARK_GRAY.shade(0.4)
                        strokeWeight = 0.2

                        val skip = window1.height / (nBlinds-1)

                        for (i in 0 until clamp((settings.blind1Closed*nBlinds).toInt()+1, 0, nBlinds)) {
                            fill = blindColors1[i]
                            val y = lid1.y + i * skip
                            contour(Rectangle(lid1.x, y, lid1.width, skip).contour)
                        }

                        for (i in 0 until clamp((settings.blind2Closed*nBlinds).toInt()+1, 0, nBlinds)) {
                            fill = blindColors2[i]
                            val y = lid2.y + i * skip
                            contour(Rectangle(lid2.x, y, lid2.width, skip).contour)
                        }
                    }
                }
                post(effect1)
            }

            layer {
                draw {
                    drawer.composition(
                        drawComposition {
                            stroke = ColorRGBa.GREY
                            stroke = null
                            strokeWeight = 0.2
                            clipMode = ClipMode.DISABLED
                            fill = ColorRGBa.WHITE.shade(0.95)
                            contour(frame1.contour)
                            contour(frame2.contour)
                            clipMode = ClipMode.DIFFERENCE

                            fill = ColorRGBa.DARK_GRAY
                            stroke = ColorRGBa.DARK_GRAY.shade(0.4)
                            strokeWeight = 1.0
                            contour(gap11.contour)
                            contour(gap12.contour)
                            contour(gap21.contour)
                            contour(gap22.contour)

                            clipMode = ClipMode.DISABLED
                        }
                    )
                }
                post(effect2)
            }

            // Bricks
            layer {
                layer {
                    invertMask = true
                    mask {
                        drawer.apply {
                            stroke = null
                            contour(frame1.contour)
                            contour(frame2.contour)
                        }
                    }

                    draw {
                        drawer.composition(drawComposition {
                            fill = ColorRGBa.GHOST_WHITE
                            stroke = ColorRGBa.GREY
                            strokeWeight = 0.2

                            bricks.forEach {
                                it.draw(this)
                            }
                        })

                        for ((i, tear) in tears.withIndex()) {
                            val b = getBrick(tear)
                            b?.wet = 1.0
                            tears[i] += Vector2(0.0, tearSpeed)
                            if (tears[i].y >= height) {
                                tears[i] = tears[i].copy(y = window2.corner.y + window2.height)
                            }
                        }
                    }
                }
                post(effect3)
            }

            layer {
                draw {
                    drawer.apply {
                        stroke = ColorRGBa.BLACK
                        strokeWeight = 0.1

                        for (i in 0 until nBottomLid) {
                            fill = bottomLidColors1[i]
                            val skip = frame1.width / nBottomLid
                            val start = Vector2(frame1.x + i * skip, frame1.y + frame1.height)
                            contour(Rectangle(start.x, start.y + 10.0, skip, 20.0).contour)
                        }
                        for (i in 0 until nBottomLid) {
                            fill = bottomLidColors1[i+nBottomLid]
                            val skip = frame2.width / nBottomLid
                            val start = Vector2(frame2.x + i * skip, frame2.y + frame2.height)
                            contour(Rectangle(start.x, start.y + 10.0, skip, 20.0).contour)
                        }
                    }
                }
                post(effect4)
            }

            layer {
                draw {
                    drawer.apply {
                        stroke = ColorRGBa.BLACK
                        fill = ColorRGBa.BURLY_WOOD
                        strokeWeight = 0.1

                        for (i in 0 until nBottomLid) {
                            fill = bottomLidColors2[i+0]
                            val skip = frame1.width / nBottomLid
                            val start = Vector2(frame1.x + i * skip, frame1.y + frame1.height)
                            contour(Rectangle(start.x, start.y, skip, 10.0).contour)
                        }
                        for (i in 0 until nBottomLid) {
                            fill = bottomLidColors2[i+nBottomLid]
                            val skip = frame2.width / nBottomLid
                            val start = Vector2(frame2.x + i * skip, frame2.y + frame2.height)
                            contour(Rectangle(start.x, start.y, skip, 10.0).contour)
                        }
                    }
                }
            }
        }

        extend(gui)
//        extend(ScreenRecorder()){
//            frameRate = 50
//            profile = GIFProfile()
//            frameSkip = 120
////            timeOffset = 3.0
//            maximumFrames = ((800.0 - frame2.y - frame2.height)/tearSpeed).toLong()
//        }

        extend {
            if (!settings.hasAnimations() && frameCount >= 120) {
                settings.apply {
                    blind1Closed = 0.5
                    blind2Closed = 0.6

                    delay(800)
                    ::blind2Closed.animate(1.0, 1800, Easing.CubicOut).completed.listen {
                        ::blind2Closed.animate(0.6, 2000, Easing.CubicOut)

                    }
                    delay(50)
                    ::blind1Closed.animate(1.0, 1800, Easing.CubicOut).completed.listen {
                        ::blind1Closed.animate(0.5, 2000, Easing.CubicOut)

                    }
                }
            }
            settings.updateAnimation()
            c.draw(drawer)
        }
    }
}