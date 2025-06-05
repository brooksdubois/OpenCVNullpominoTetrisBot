import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt

fun DoubleArray.toHSV(): Triple<Double, Double, Double> {
    val bgrMat = Mat(1, 1, CvType.CV_8UC3)
    bgrMat.put(0, 0, byteArrayOf(
        this[0].roundToInt().coerceIn(0, 255).toByte(),
        this[1].roundToInt().coerceIn(0, 255).toByte(),
        this[2].roundToInt().coerceIn(0, 255).toByte()
    ))

    val hsvMat = Mat()
    Imgproc.cvtColor(bgrMat, hsvMat, Imgproc.COLOR_BGR2HSV)
    val hsv = hsvMat.get(0, 0)
    return Triple(hsv[0], hsv[1], hsv[2])
}