import ai.TurnExecutor
import engine.TetrisBoard
import input.MoveMapper
import opencv.bufferedImageToMat
import opencv.detectNextPieceColor
import opencv.drawRect
import opencv.hasColorChanged
import org.opencv.core.*
import org.opencv.highgui.HighGui
import java.awt.Rectangle
import java.awt.Robot

import tetris.opencv.*

fun main() {
    System.load("${System.getProperty("user.dir")}/libopencv_java4110.dylib")

    val robot = Robot()
    val board = TetrisBoard()
    val mapper = MoveMapper()
    val executor = TurnExecutor(robot, mapper)

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
        val screenCapture = robot.createScreenCapture(gameRegion)
        val mat = bufferedImageToMat(screenCapture)

        board.drawOverlay(mat, boardTopLeft, cellSize)
        board.drawDebugOverlay(mat, boardTopLeft, cellSize)
        drawRect(mat, nextBoxRect, Scalar(0.0, 255.0, 0.0), 2)

        val nextPieceColor = detectNextPieceColor(mat, nextBoxRect)
        if (hasColorChanged(lastColor, nextPieceColor, 25.0)) {
            val now = System.currentTimeMillis()
            if (now - lastPlayTime > minDelayMs) {
                val tetromino = classifyPieceColor(nextPieceColor)
                if (tetromino != null) {
                    board.updateFromGameFrame(mat, boardTopLeft, cellSize)
                    println("Next piece: $tetromino")
                    executor.playTurn(board, tetromino)
                    lastColor = nextPieceColor
                    lastPlayTime = now
                }
            }
        }

        HighGui.imshow("Nullpomino", mat)
        if (HighGui.waitKey(33) >= 0) break
    }
}
