package opencv

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import kotlin.math.sqrt

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

fun detectNextPieceColor(mat: Mat, nextBoxRect: Rect): Scalar {
    val sub = mat.submat(nextBoxRect)
    var sumR = 0.0; var sumG = 0.0; var sumB = 0.0; var count = 0
    for (y in 0 until sub.rows()) {
        for (x in 0 until sub.cols()) {
            val pixel = sub.get(y, x)
            val (b, g, r) = pixel
            if (r + g + b < 50) continue
            sumB += b; sumG += g; sumR += r; count++
        }
    }
    return if (count > 0) Scalar(sumB / count, sumG / count, sumR / count)
    else Scalar(0.0, 0.0, 0.0)
}

fun hasColorChanged(prev: Scalar?, curr: Scalar, threshold: Double): Boolean {
    if (prev == null) return true
    val dr = curr.`val`[2] - prev.`val`[2]
    val dg = curr.`val`[1] - prev.`val`[1]
    val db = curr.`val`[0] - prev.`val`[0]
    val dist = sqrt(dr * dr + dg * dg + db * db)
    return dist > threshold
}

fun drawRect(mat: Mat, rect: Rect, color: Scalar, thickness: Int = 2) {
    Imgproc.rectangle(
        mat,
        Point(rect.x.toDouble(), rect.y.toDouble()),
        Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
        color,
        thickness
    )
}
