import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.highgui.HighGui
import java.awt.Rectangle
import java.awt.Robot
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

val pieceColorMap = mapOf(
    "O" to Scalar(22.2, 209.4, 156.8),
    "S" to Scalar(28.0, 219.7, 86.2),
    "Z" to Scalar(29.8, 135.8, 168.8),
    "T" to Scalar(155.4, 110.0, 151.9),
    "L" to Scalar(34.7, 184.0, 164.4),
    "J" to Scalar(159.2, 141.1, 46.1),
    "I" to Scalar(153.8, 208.7, 22.9)
)

fun classifyPieceColor(color: Scalar): String {
    var closestPiece = "Unknown"
    var minDistance = Double.MAX_VALUE
    for ((piece, refColor) in pieceColorMap) {
        val dr = color.`val`[2] - refColor.`val`[2]
        val dg = color.`val`[1] - refColor.`val`[1]
        val db = color.`val`[0] - refColor.`val`[0]
        val dist = dr * dr + dg * dg + db * db
        if (dist < minDistance) {
            minDistance = dist
            closestPiece = piece
        }
    }
    return closestPiece
}

fun hasColorChanged(prev: Scalar?, curr: Scalar, threshold: Double): Boolean {
    if (prev == null) return true
    val dr = curr.`val`[2] - prev.`val`[2]
    val dg = curr.`val`[1] - prev.`val`[1]
    val db = curr.`val`[0] - prev.`val`[0]
    val dist = sqrt(dr * dr + dg * dg + db * db)
    return dist > threshold
}

fun playTurn(
    board: TetrisBoard,
    tetrominoName: String,
    robot: Robot,
    moveMapper: MoveMapper = MoveMapper()
) {
    val tetromino = try {
        Tetromino.valueOf(tetrominoName)
    } catch (e: IllegalArgumentException) {
        println("Unknown tetromino: $tetrominoName")
        return
    }

    val best = board.autoSelect(tetromino)
    if (best != null) {
        val (rotation, column) = best
        println("→ Placing $tetromino at col $column, rot $rotation")
        val inputs = moveMapper.generateInputSequence(rotation, column)
        moveMapper.execute(robot, inputs)
        board.dropPiece(tetromino, rotation, column)
        board.printBoard()
        Thread.sleep(500)
    } else {
        println("× No valid placement found for $tetromino")
    }
}

fun main() {
    System.load("${System.getProperty("user.dir")}/libopencv_java4110.dylib")

    val robot = Robot()
    val board = TetrisBoard()
    val mapper = MoveMapper()
    val gameRegion = Rectangle(160, 140, 350, 480)
    val nextBoxRect = Rect(200, 20, 70, 40)

    val boardTopLeft = Point(160.0, 63.0)
    val cellSize = Size(16.0, 16.0)

    var lastColor: Scalar? = null
    var lastPlayTime = System.currentTimeMillis()
    val minDelayMs = 1000L

    println("⏳ Waiting 3 seconds before starting bot...")
    Thread.sleep(3000)
    println("✅ Bot starting.")

    while (true) {
        val screenCapture: BufferedImage = robot.createScreenCapture(gameRegion)
        val mat = bufferedImageToMat(screenCapture)

        // Visual overlays
        board.drawOverlay(mat, boardTopLeft, cellSize)
        Imgproc.rectangle(
            mat,
            Point(nextBoxRect.x.toDouble(), nextBoxRect.y.toDouble()),
            Point((nextBoxRect.x + nextBoxRect.width).toDouble(), (nextBoxRect.y + nextBoxRect.height).toDouble()),
            Scalar(0.0, 255.0, 0.0),
            2
        )

        // Detect and react
        val nextPieceColor = detectNextPieceColor(mat, nextBoxRect)
        if (hasColorChanged(lastColor, nextPieceColor, 25.0)) {
            val now = System.currentTimeMillis()
            if (now - lastPlayTime > minDelayMs) {
                val pieceName = classifyPieceColor(nextPieceColor)

                // 🧠 Sync visual board state
                board.updateFromGameFrame(mat, boardTopLeft, cellSize)

                println("Next piece: $pieceName")
                playTurn(board, pieceName, robot, mapper)

                lastColor = nextPieceColor
                lastPlayTime = now
            }
        }

        HighGui.imshow("Nullpomino", mat)
        if (HighGui.waitKey(33) >= 0) break
    }
}
