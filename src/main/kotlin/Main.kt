import engine.TetrisBoard
import input.MoveMapper
import opencv.bufferedImageToMat
import opencv.detectNextPieceColor
import opencv.detectPlayfield
import opencv.drawRect
import opencv.hasColorChanged
import org.opencv.core.*
import org.opencv.highgui.HighGui
import java.awt.Rectangle
import java.awt.Robot
import engine.Tetromino
import tetris.opencv.*

fun main() {
    System.load("${System.getProperty("user.dir")}/libopencv_java4110.dylib")

    val robot = Robot()
    val board = TetrisBoard()
    val mapper = MoveMapper()

    val gameRegion = Rectangle(160, 140, 350, 480)
    val nextBoxRect = Rect(200, 20, 70, 40)

    var lastColor: Scalar? = null
    var lastPlayTime = System.currentTimeMillis()
    val minDelayMs = 1000L

    println("⏳ Waiting 3 seconds before starting bot...")
    Thread.sleep(3000)
    println("✅ Bot starting.")

    var currentPiece: Tetromino? = null
    var nextPiece: Tetromino? = null

    val screenCapture = robot.createScreenCapture(gameRegion)
    val mat = bufferedImageToMat(screenCapture)

    val detected = detectPlayfield(mat)
    if (detected == null) {
        println("⚠️ Could not detect playfield.")
        HighGui.imshow("Nullpomino", mat)
        if (HighGui.waitKey(33) >= 0) return
       // continue
        return
    }

    val (boardTopLeft, cellSize) = detected
    board.drawOverlay(mat, boardTopLeft, cellSize)

    while (true) {
        val screenCapture = robot.createScreenCapture(gameRegion)
        val mat = bufferedImageToMat(screenCapture)
        drawRect(mat, nextBoxRect, Scalar(0.0, 255.0, 0.0), 2)

        val nextPieceColor = detectNextPieceColor(mat, nextBoxRect)
        board.updateFromGameFrame(mat, boardTopLeft, cellSize)

        if (hasColorChanged(lastColor, nextPieceColor, 25.0)) {
            val now = System.currentTimeMillis()
            if (now - lastPlayTime > minDelayMs) {
                val detected = classifyPieceColor(nextPieceColor)
                if (detected != null) {
                    currentPiece = nextPiece
                    nextPiece = detected

                    if (currentPiece != null) {
                        println("Current: $currentPiece | Next: $nextPiece")

                        val best = board.autoSelect(currentPiece, nextPiece)
                        val (rotation, column) = best

                        board.drawBotPrediction(mat, boardTopLeft, cellSize, currentPiece, rotation, column)
                        println("🧠 Playing $currentPiece at column $column, rot $rotation")

                        mapper.execute(robot, mapper.generateInputSequence(rotation, column))
                        lastColor = nextPieceColor
                        lastPlayTime = now
                    } else {
                        println("First piece: $detected (waiting for next...)")
                    }
                }
            }
        }

        HighGui.imshow("Nullpomino", mat)
        if (HighGui.waitKey(33) >= 0) break

        Thread.sleep(100)
    }

}
