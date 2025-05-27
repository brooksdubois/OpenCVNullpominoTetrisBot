package opencv

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

fun detectPlayfield(mat: Mat): Pair<Point, Size>? {
    val hsv = Mat()
    Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

    val lowerGreen = Scalar(50.0, 100.0, 100.0)
    val upperGreen = Scalar(90.0, 255.0, 255.0)
    val mask = Mat()
    Core.inRange(hsv, lowerGreen, upperGreen, mask)

    val contours = mutableListOf<MatOfPoint>()
    Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

    val rect = contours
        .map { Imgproc.boundingRect(it) }
        .filter { it.width > 100 && it.height > 200 }
        .maxByOrNull { it.width * it.height } ?: return null

    // Shrink sides: left +1, right +10, top/bottom +1
    val x = rect.x + 1
    val y = rect.y + 1
    val w = rect.width - 10
    val h = rect.height - 2  // 1 top + 1 bottom

    val origin = Point(x.toDouble(), y.toDouble())
    val cellSize = Size(w.toDouble() / 10.0, h.toDouble() / 20.0)

    return origin to cellSize
}
