import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class SimpleHuePieceDetector(
    private val boxes: List<Rect>
) {
    private var previousQueue: List<Brick?> = emptyList()
    private var currentQueue: List<Brick?> = emptyList()
    var current: Brick? = null
        private set

    fun update(mat: Mat) {
        val detected = boxes.map { box ->
            drawRect(mat, box, Scalar(0.0, 255.0, 0.0), 2)
            val hue = getAverageHue(mat, box)
            hue?.let { BrickHue.fromHue(it) }
        }

        // Promote the previous front piece if the back changed
        if (previousQueue.isNotEmpty() && detected.lastOrNull() != previousQueue.lastOrNull()) {
            val justDropped = previousQueue.firstOrNull()
            if (justDropped != null && justDropped != current) {
                current = justDropped
                println("Current piece: $current")
            }
        }

        currentQueue = detected
        previousQueue = detected
    }

    fun getQueue(): List<Brick?> = currentQueue

    private fun getAverageHue(mat: Mat, rect: Rect): Double? {
        val hsv = Mat().also {
            Imgproc.cvtColor(mat.submat(rect), it, Imgproc.COLOR_BGR2HSV)
        }

        val marginX = rect.width / 4
        val marginY = rect.height / 4

        val region = (marginY until (rect.height - marginY)).flatMap { y ->
            (marginX until (rect.width - marginX)).mapNotNull { x ->
                hsv.get(y, x)?.takeIf { it[2] >= 40 }?.get(0)
            }
        }

        return region.takeIf { it.isNotEmpty() }?.average()
    }

    private fun drawRect(mat: Mat, rect: Rect, color: Scalar, thickness: Int = 2) {
        Imgproc.rectangle(mat, rect.tl(), rect.br(), color, thickness)
    }
}
