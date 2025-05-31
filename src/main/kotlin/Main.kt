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

    val gameRegion = Rectangle(260, 240, 380, 480)
    val nextBoxRect = Rect(200, 20, 70, 40)
    val secondNextBox = Rect(273, 40, 36, 20)
    val thirdNextBox = Rect(312, 40, 36, 20)
    val fourthNextBox = Rect(326, 80, 36, 20)

    var lastColor: Scalar? = null
    var lastPlayTime = System.currentTimeMillis()
    val minDelayMs = 1000L
    var isFirstTurn = true

    println("⏳ Waiting 3 seconds before activating bot...")
    Thread.sleep(3000)

    val screenCapture = robot.createScreenCapture(gameRegion)
    val mat = bufferedImageToMat(screenCapture)
    val detected = detectPlayfield(mat)

    if (detected == null) {
        println("⚠️ Could not detect playfield.")
        HighGui.imshow("Nullpomino", mat)
        if (HighGui.waitKey(33) >= 0) return
        return
    }

    val (boardTopLeft, cellSize) = detected
    board.drawOverlay(mat, boardTopLeft, cellSize)

    // ✅ Wait for game to visually start
    println("⏳ Waiting for game to start...")
    while (true) {
        val startCapture = robot.createScreenCapture(gameRegion)
        val startMat = bufferedImageToMat(startCapture)
        board.updateFromGameFrame(startMat, boardTopLeft, cellSize)

        val hasLiveBlocks = (0 until board.height).any { row ->
            (0 until board.width).any { col ->
                board.isCellOccupied(row, col)
            }
        }

        if (hasLiveBlocks) break

        HighGui.imshow("Waiting for start", startMat)
        if (HighGui.waitKey(33) >= 0) return
        Thread.sleep(100)
    }

    println("✅ Game detected. Bot starting.")

    var currentPiece: Tetromino? = null
    var nextPiece: Tetromino? = null
    var secondNextPiece: Tetromino? = null
    var thirdNextPiece: Tetromino? = null
    var fourthNextPiece: Tetromino? = null

    while (true) {
        val screenCapture = robot.createScreenCapture(gameRegion)
        val mat = bufferedImageToMat(screenCapture)
        drawRect(mat, nextBoxRect, Scalar(0.0, 255.0, 0.0), 2)
        drawRect(mat, secondNextBox, Scalar(0.0, 255.0, 0.0), 2)
        drawRect(mat, thirdNextBox, Scalar(0.0, 255.0, 0.0), 2)
        drawRect(mat, fourthNextBox, Scalar(0.0, 255.0, 0.0), 2)

        val nextPieceColor = detectNextPieceColor(mat, nextBoxRect)
        val secondNextPieceColor = detectNextPieceColor(mat, secondNextBox)
        val thirdNextPieceColor = detectNextPieceColor(mat, thirdNextBox)
        val fourthNextPieceColor = detectNextPieceColor(mat, fourthNextBox)

        board.updateFromGameFrame(mat, boardTopLeft, cellSize)

        if (hasColorChanged(lastColor, nextPieceColor, 25.0)) {
            val now = System.currentTimeMillis()
            if (now - lastPlayTime > minDelayMs) {
                val firstDetected = classifyPieceColor(nextPieceColor)
                val secondDetected = classifyPieceColor(secondNextPieceColor)
                val thirdDetected = classifyPieceColor(thirdNextPieceColor)
                val fourthDetected = classifyPieceColor(fourthNextPieceColor)

                if (firstDetected != null && secondDetected != null && thirdDetected != null && fourthDetected != null) {
                    if (isFirstTurn) {
                        nextPiece = firstDetected
                        secondNextPiece = secondDetected
                        thirdNextPiece = thirdDetected
                        fourthNextPiece = fourthDetected
                        println("First piece: $nextPiece (waiting for drop...)")
                        isFirstTurn = false
                    } else {
                        currentPiece = nextPiece
                        nextPiece = secondNextPiece
                        secondNextPiece = thirdNextPiece
                        thirdNextPiece = fourthNextPiece
                        fourthNextPiece = fourthDetected

                        if (currentPiece != null) {
                            println("Current: $currentPiece | Next: $nextPiece | Second: $secondNextPiece | Third: $thirdNextPiece")

                            val move = board.autoSelectWithLookahead(
                                listOfNotNull(currentPiece, nextPiece, secondNextPiece),
                                depth = 2
                            )

                            if (move != null) {
                                val (rotation, column) = move
                                board.drawBotPrediction(mat, boardTopLeft, cellSize, currentPiece, rotation, column)

                                val dropped = board.dropPiece(currentPiece, rotation, column)
                                if (!dropped) println("❌ Drop failed — possibly out of sync")

                                mapper.execute(robot, mapper.generateInputSequence(rotation, column))
                                lastColor = nextPieceColor
                                lastPlayTime = now
                            }
                        }
                    }
                }
            }
        }

        HighGui.imshow("Nullpomino", mat)
        if (HighGui.waitKey(33) >= 0) break

        Thread.sleep(100)
    }
}
