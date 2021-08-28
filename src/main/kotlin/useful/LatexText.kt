package useful

import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGeneratorContext
import org.apache.batik.svggen.SVGGraphics2D
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.shape.Composition
import org.openrndr.svg.loadSVG
import org.scilab.forge.jlatexmath.DefaultTeXFont
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import org.scilab.forge.jlatexmath.cyrillic.CyrillicRegistration
import org.scilab.forge.jlatexmath.greek.GreekRegistration
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
import java.io.*
import javax.swing.JLabel

import java.security.MessageDigest

class LatexText {
    val filePath: String
    val composition: Composition

    @Throws(IOException::class)
    constructor(latex: String?, size: Float) {
        // Make file name the SHA-256 hash of the latex string + the size
        val bytes = (latex + size.toString()).toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val fileName = digest.fold("") { str, it -> str + "%02x".format(it) }
        filePath = "tmp/$fileName.svg"

        // Check whether file already exists
        val file = File(filePath)
        if (file.exists()){
            composition = loadSVG(filePath)
            composition.findShapes().map { it.stroke = ColorRGBa.BLACK }
        } else {
            val domImpl = GenericDOMImplementation.getDOMImplementation()
            val svgNS = "http://www.w3.org/2000/svg"
            val document = domImpl.createDocument(svgNS, "svg", null)
            val ctx = SVGGeneratorContext.createDefault(document)
            val fontAsShapes = true
            val g2 = SVGGraphics2D(ctx, fontAsShapes)
            DefaultTeXFont.registerAlphabet(CyrillicRegistration())
            DefaultTeXFont.registerAlphabet(GreekRegistration())
            val formula = TeXFormula(latex)
            val icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, size)
            icon.insets = Insets(5, 5, 5, 5)
            g2.svgCanvasSize = Dimension(icon.iconWidth, icon.iconHeight
            )

            val jl = JLabel()
            jl.foreground = Color(0, 0, 0)
            icon.paintIcon(jl, g2, 0, 0)
            val useCSS = true

            val svgs = FileOutputStream(filePath)
            val out: Writer = OutputStreamWriter(svgs, "UTF-8")
            g2.stream(out, useCSS)
            svgs.flush()
            svgs.close()

            composition = loadSVG(filePath)
            composition.findShapes().map { it.stroke = ColorRGBa.BLACK }
        }
    }
}

fun Drawer.text(text: LatexText, x: Double, y: Double){
    isolated {
        translate(x, y)
        composition(text.composition)
    }
}