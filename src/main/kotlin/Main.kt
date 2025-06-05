import TetrisOverlay.cellStateGridToBool
import TetrisOverlay.extractCellStatesFromFrame
import org.opencv.core.Rect
import org.opencv.highgui.HighGui
import java.awt.*
import java.awt.event.KeyEvent

fun printGrid(grid: Array<BooleanArray>) {
    println("📦 Grid:")
    for (row in grid) {
        println(row.joinToString("") { if (it) "#" else "." })
    }
}

fun main() {
    System.load("${System.getProperty("user.dir")}/libopencv_java4110.dylib")

    val robot = Robot()
    val board = TetrisBoard()
    val mapper = MoveMapper(delayMs = 75L, rotateFirst = true)
    setupKeyListener()

    val gameRegion = Rectangle(260, 240, 380, 480)

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
    TetrisOverlay.drawOverlay(mat, boardTopLeft, cellSize, board.width, board.height)

    println("✅ Game detected. Bot starting.")
    val detector = SimpleHuePieceDetector(
        listOf(
            Rect(200, 20, 70, 40),
            Rect(273, 40, 36, 20),
            Rect(312, 40, 36, 20),
            Rect(326, 80, 36, 20)
        )
    )

    var lastLoggedQueue: List<Brick?> = emptyList()
    var lastPlannedPiece: Brick? = null
    var currentMove: Pair<Int, Int>? = null

    while (true) {
        val mat = bufferedImageToMat(robot.createScreenCapture(gameRegion))

        val cellStates = extractCellStatesFromFrame(mat, boardTopLeft, cellSize, board.width, board.height)
        TetrisOverlay.drawCellOverlays(mat, boardTopLeft, cellSize, cellStates)

        val cellGrid = cellStateGridToBool(cellStates)
        board.updateFromGameFrame(cellGrid)
        detector.update(mat)

        val queue = detector.getQueue()
        if (queue != lastLoggedQueue) {
            println("Next pieces: $queue")
            lastLoggedQueue = queue
        }

        val currentPiece = detector.current
        if (currentPiece != null && currentPiece != lastPlannedPiece) {
            val grid = board.getGrid()
            printGrid(grid)

            val move = autoSelectWithLookaheadBasicAI(board, listOf(currentPiece))
            println("🧠 AI selected move for $currentPiece → $move")
            currentMove = move
            lastPlannedPiece = currentPiece

            if (move != null) {
                val (rotation, column) = move
                val inputs = mapper.generateInputSequence(rotation, column, mapper.spawnColumnFor(currentPiece), currentPiece)
                mapper.execute(robot, inputs)
            }
        }

        if (currentPiece != null && currentMove != null) {
            val (rotation, column) = currentMove
            TetrisOverlay.drawBotPrediction(
                mat,
                boardTopLeft,
                cellSize,
                currentPiece,
                rotation,
                column,
                board.getGrid()
            )
        }

        HighGui.imshow("Tetris", mat)
        if (HighGui.waitKey(33) >= 0) break

        Thread.sleep(300)
    }
}
