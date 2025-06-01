import TetrisOverlay.cellStateGridToBool
import TetrisOverlay.extractCellStatesFromFrame
import org.opencv.core.*
import org.opencv.highgui.HighGui
import java.awt.Rectangle
import java.awt.Robot

fun main() {
    System.load("${System.getProperty("user.dir")}/libopencv_java4110.dylib")

    val robot = Robot()
    val board = TetrisBoard()
    val mapper = MoveMapper()
    setupKeyListener()

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

    // Detect playfield once
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
    TetrisOverlay.drawOverlay(mat, boardTopLeft, cellSize, board.width, board.height)

    // Wait for game to start visually
    println("⏳ Waiting for game to start...")
    while (true) {
        val startCapture = robot.createScreenCapture(gameRegion)
        val startMat = bufferedImageToMat(startCapture)

        val cellStates = extractCellStatesFromFrame(startMat, boardTopLeft, cellSize, board.width, board.height)
        TetrisOverlay.drawCellOverlays(startMat, boardTopLeft, cellSize, cellStates)

        val cellGrid = cellStateGridToBool(cellStates)
        board.updateFromGameFrame(cellGrid)

        val hasLiveBlocks = cellGrid.any { row -> row.any { it } }
        if (hasLiveBlocks) break

        HighGui.imshow("Waiting for start", startMat)
        if (HighGui.waitKey(33) >= 0) return
        Thread.sleep(100)
    }

    println("✅ Game detected. Bot starting.")

    var currentPiece: Brick? = null
    var nextPiece: Brick? = null
    var secondNextPiece: Brick? = null
    var thirdNextPiece: Brick? = null
    var fourthNextPiece: Brick? = null

    while (true) {
        if (resetRequested) {
            println("🔁 Resetting board...")
            board.updateFromGameFrame(Array(board.height) { BooleanArray(board.width) })
            currentPiece = null
            nextPiece = null
            secondNextPiece = null
            thirdNextPiece = null
            fourthNextPiece = null
            isFirstTurn = true
            resetRequested = false

            println("⏳ Waiting for game to restart...")
            continue  // skip this frame
        }
        val screenCapture = robot.createScreenCapture(gameRegion)
        val mat = bufferedImageToMat(screenCapture)

        // --- Board state extraction and overlay (always!) ---
        val cellStates = extractCellStatesFromFrame(mat, boardTopLeft, cellSize, board.width, board.height)
        TetrisOverlay.drawCellOverlays(mat, boardTopLeft, cellSize, cellStates)

        val cellGrid = cellStateGridToBool(cellStates)
        board.updateFromGameFrame(cellGrid)

        // Draw piece detection rectangles (debug)
        drawRect(mat, nextBoxRect, Scalar(0.0, 255.0, 0.0), 2)
        drawRect(mat, secondNextBox, Scalar(0.0, 255.0, 0.0), 2)
        drawRect(mat, thirdNextBox, Scalar(0.0, 255.0, 0.0), 2)
        drawRect(mat, fourthNextBox, Scalar(0.0, 255.0, 0.0), 2)

        // Detect upcoming piece colors
        val nextPieceColor = detectNextPieceColor(mat, nextBoxRect)
        val secondNextPieceColor = detectNextPieceColor(mat, secondNextBox)
        val thirdNextPieceColor = detectNextPieceColor(mat, thirdNextBox)
        val fourthNextPieceColor = detectNextPieceColor(mat, fourthNextBox)

        // Only proceed if the next piece changes visually
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
                            val move = TetrisMoves.autoSelectWithLookahead(
                                board,
                                listOfNotNull(currentPiece, nextPiece, secondNextPiece),
                                depth = 2
                            ) { grid -> TetrisMoves.evaluateBoardStatic(grid) }

                            if (move != null) {
                                val (rotation, column) = move
                                TetrisOverlay.drawBotPrediction(
                                    mat, boardTopLeft, cellSize,
                                    currentPiece, rotation, column, board.getGrid()
                                )
                                // Drop the piece on the board (in the bot simulation)
                                val dropRow = (board.height downTo 0).firstOrNull { row ->
                                    board.canPlace(currentPiece, rotation, row to column) &&
                                            board.hasVerticalAccess(currentPiece, rotation, row to column)
                                }
                                if (dropRow != null) {
                                    board.place(currentPiece, rotation, dropRow to column)
                                    board.clearFullLines()
                                } else {
                                    println("❌ Drop failed — possibly out of sync")
                                }
                                // Send moves to game window
                                val spawnCol = mapper.spawnColumnFor(currentPiece)
                                val inputs = mapper.generateInputSequence(rotation, column, spawnCol)
                                mapper.execute(robot, inputs)
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
