package opencv

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt

fun colorName(h: Double, s: Double, v: Double): String {
    if (v < 40) return "black"
    if (s < 40) return when {
        v < 100 -> "dark gray"
        v > 200 -> "light gray"
        else -> "gray"
    }

    val shade = when {
        v < 100 -> "dark "
        v > 200 -> "light "
        else -> ""
    }

    val base = when (h) {
        in 0.0..10.0, in 170.0..180.0 -> "red"
        in 11.0..30.0 -> "orange"
        in 31.0..50.0 -> "yellow"
        in 51.0..85.0 -> "green"
        in 86.0..120.0 -> "cyan"
        in 121.0..150.0 -> "blue"
        in 151.0..169.0 -> "purple"
        else -> "unknown"
    }

    return shade + base
}

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