import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.shadow.DropShadow
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.extra.shadestyles.radialGradient
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.extras.color.presets.*
import org.openrndr.extras.color.spaces.ColorXSLUVa
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.map
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        val gui = GUI()
        val duration = 3.0

        // Star settings
        val points = poissonDiskSampling(width*5.0, height*5.0, 250.0, 400, false).map {
            it + Vector2(-1.5*width, -1.5*height)
            it + Vector2(-1.5*width, -1.5*height)
        }
        val stars = points.map { FiveStar(it, 10.0, 5.0) }

        val filmGrain = FilmGrain()
        val chromaticAberration = ChromaticAberration()
        val bloom = GaussianBloom()
        filmGrain.addTo(gui)
        chromaticAberration.addTo(gui)
        chromaticAberration.aberrationFactor = 1.5
        bloom.addTo(gui)
        bloom.sigma = 1.0
        bloom.window = 1
        bloom.gain = 4.0
//        bloom.gain = 2.0
        bloom.shape = 1.0

        // Doorway settings
        val rep = 1.4
        val w = width*1.0
        val h = w*1.7
        val door = Rectangle(width/2.0 - w/2.0, height*-0.1, w, h)
        val perspectiveLineTopRight = LineSegment(door.corner, Vector2(width/2.0, height*0.6))
        val perspectiveLineBottomLeft = LineSegment(door.corner + Vector2(0.0, door.height), Vector2(width/2.0, height*0.6))

        val c = compose {
            val ds = DropShadow()
            ds.addTo(gui)

            layer {
                for (j in 6 downTo -2) {
                    layer {
                        draw {
//                        val t = 1-exp(-i*0.4 + (seconds/duration % 1))
                            val i = j - seconds / duration % 1
//                        val i = j
//                        val pt = 1-exp(-(i-0)*rep)
                            val t = 1 - exp(-i * rep)
                            val tr = perspectiveLineTopRight.position(t)
                            val bl = perspectiveLineBottomLeft.position(t)
                            val rect = Rectangle(tr.x, tr.y, (door.center.x - tr.x) * 2, bl.y - tr.y)
//                        val bl = rect.corner + Vector2(0.0, rect.height)

                            drawer.apply {
//                                clear(ColorRGBa(0.1, 0.0, 0.19).shade((1 - t.pow(3.0)).clamp(0.2, 0.7)))
//                                fill = ColorRGBa(0.1, 0.0, 0.19).shade((1 - t.pow(3.0)).clamp(0.2, 0.7))
//                                rectangle(0.0, 0.0, width*1.0, bl.y)
//                                erase {
//                                    contour(rect.contour)
//                                }
                            }
//
//                        drawer.apply {
//                            circle(bl, 5.0)
//                            lineSegment(bl)
//                        }
                        }

                        layer {
                            draw {
                                val i = j - seconds / duration % 1
                                val t = 1 - exp(-i * rep)
                                val tr = perspectiveLineTopRight.position(t)
                                val bl = perspectiveLineBottomLeft.position(t)
                                val rect = Rectangle(tr.x, tr.y, (door.center.x - tr.x) * 2, bl.y - tr.y)
                                val arcDoor = contour {
                                    moveTo(rect.corner + Vector2(0.0, rect.height))
                                    curveTo(rect.corner, rect.center - Vector2(0.0, rect.height/2.0))
                                }.plus(contour {
                                    moveTo(rect.corner + Vector2(rect.width, rect.height))
                                    curveTo(rect.corner + Vector2(rect.width, 0.0), rect.center - Vector2(0.0, rect.height/2.0))
                                }.reversed)

                                drawer.isolated {
                                    isolated {
                                        translate(Vector2(width / 2.0, height * 0.6))
                                        val sc = rect.width / w * 3
                                        scale(sc)
                                        translate(-Vector2(width / 2.0, height * 0.6))
                                        stars.forEachIndexed { i, star ->
                                            isolated {
                                                translate(Vector2(width / 2.0, height * 0.6))
//                                                scale(map(0.0, stars.size*1.0, 0.3, 3.0, i*1.0))
                                                scale(simplex(0, i.toDouble())+0.2)
                                                translate(-Vector2(width / 2.0, height * 0.6))
                                                star.draw(
                                                    this,
                                                    star.color.shade((1 - t.pow(3.0)).clamp(0.0, 0.7))
                                                )
                                            }
                                        }
                                    }

//                                    erase {
//                                        contour(arcDoor)
//                                        rectangle(0.0, bl.y, width * 1.0, height - bl.y)
//                                    }
                                }
                            }
                            post(bloom)
                        }

                        layer {
                            layer {
                                draw {
                                    val i = j - seconds / duration % 1
                                    val t = 1 - exp(-i * rep)
                                    val tr = perspectiveLineTopRight.position(t)
                                    val bl = perspectiveLineBottomLeft.position(t)
                                    val rect = Rectangle(tr.x, tr.y, (door.center.x - tr.x) * 2, bl.y - tr.y)
                                    val arcDoor = contour {
                                        moveTo(rect.corner + Vector2(0.0, rect.height))
                                        curveTo(rect.corner, rect.center - Vector2(0.0, rect.height/2.0))
                                    }.plus(contour {
                                        moveTo(rect.corner + Vector2(rect.width, rect.height))
                                        curveTo(rect.corner + Vector2(rect.width, 0.0), rect.center - Vector2(0.0, rect.height/2.0))
                                    }.reversed)

                                    drawer.stroke = ColorRGBa.WHITE
                                    val sc = rect.width / w * 10
                                    drawer.strokeWeight = sc
                                    drawer.contour(arcDoor)
                                }
                            }

                            layer {
                                draw {
                                    val i = j - seconds / duration % 1
                                    val t = 1 - exp(-i * rep)
                                    val tr = perspectiveLineTopRight.position(t)
                                    val bl = perspectiveLineBottomLeft.position(t)
                                    val rect = Rectangle(tr.x, tr.y, (door.center.x - tr.x) * 2, bl.y - tr.y)

                                    drawer.isolated {
                                        translate(Vector2(width / 2.0, height * 0.6))
                                        val sc = rect.width / w * 3
                                        scale(sc)
                                        translate(-Vector2(width / 2.0, height * 0.6))

                                        fill = ColorRGBa.WHITE_SMOKE
                                        circle(door.center - Vector2(0.0, door.height * 0.17), 50.0)

                                        erase {
                                            circle(
                                                door.center - Vector2((i/2.0 + 1.0) * 50.0 - 25.0, door.height * 0.19),
                                                90.0
                                            )
                                        }

                                        fill = null
                                        stroke = ColorRGBa.WHITE_SMOKE
                                        circle(door.center - Vector2(0.0, door.height * 0.17), 60.0)
                                    }
                                }
                                post(bloom)
                            }

                        }
//                    post(ds)
//                    post(bloom)
                    }
                }
//                post(bloom)
                post(chromaticAberration)
            }
        }


//        extend(gui)
        extend(ScreenRecorder()){
            maximumDuration = duration
            profile = GIFProfile()
            frameRate = 50
        }
        extend(TemporalBlur()){
            fps = 50.0
            this.duration = 0.95
            samples = 15
            jitter = 0.0
            linearizeInput = false
            delinearizeOutput = false
        }
        extend {
            c.draw(drawer)
        }
    }
}

fun Drawer.erase(function: Drawer.() -> Unit) {
    pushStyle()
    drawStyle.blendMode = BlendMode.REMOVE
    fill = ColorRGBa.WHITE
    stroke = null
    strokeWeight = 0.0
    isolated {
        function()
    }
    popStyle()
}