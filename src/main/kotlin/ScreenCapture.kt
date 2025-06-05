import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import kotlin.math.sqrt
import javax.swing.*
import java.awt.event.*

fun bufferedImageToMat(bi: BufferedImage): Mat {
    val converted = BufferedImage(bi.width, bi.height, BufferedImage.TYPE_3BYTE_BGR)
    val g = converted.createGraphics()
    g.drawImage(bi, 0, 0, null)
    g.dispose()

    val mat = Mat(converted.height, converted.width, CvType.CV_8UC3)
    val data = (converted.raster.dataBuffer as DataBufferByte).data
    mat.put(0, 0, data)
    return mat
}

var resetRequested = false

fun setupKeyListener() {
    val frame = JFrame("TetrisBot Control")
    frame.setSize(100, 100)
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isAlwaysOnTop = true
    frame.isUndecorated = true
    frame.setLocation(400, 10)
    frame.isVisible = true

    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if (e.keyCode == KeyEvent.VK_L) {
                resetRequested = true
            }
        }
    })
}