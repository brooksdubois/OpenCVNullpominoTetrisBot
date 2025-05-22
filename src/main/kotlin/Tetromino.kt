import java.awt.Robot
import java.awt.event.KeyEvent
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


enum class Tetromino(val shape: List<List<Pair<Int, Int>>>) {
    I(listOf(
        listOf(0 to -1, 0 to 0, 0 to 1, 0 to 2),
        listOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0),
        listOf(0 to -1, 0 to 0, 0 to 1, 0 to 2),
        listOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0)
    )),
    O(listOf(
        listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
        listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
        listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
        listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
    )),
    T(listOf(
        listOf(0 to -1, 0 to 0, 0 to 1, 1 to 0),
        listOf(-1 to 0, 0 to 0, 1 to 0, 0 to -1),
        listOf(0 to -1, 0 to 0, 0 to 1, -1 to 0),
        listOf(-1 to 0, 0 to 0, 1 to 0, 0 to 1)
    )),
    S(listOf(
        listOf(0 to 0, 0 to 1, 1 to -1, 1 to 0),
        listOf(-1 to 0, 0 to 0, 0 to 1, 1 to 1),
        listOf(0 to 0, 0 to 1, 1 to -1, 1 to 0),
        listOf(-1 to 0, 0 to 0, 0 to 1, 1 to 1)
    )),
    Z(listOf(
        listOf(0 to -1, 0 to 0, 1 to 0, 1 to 1),
        listOf(-1 to 1, 0 to 0, 0 to 1, 1 to 0),
        listOf(0 to -1, 0 to 0, 1 to 0, 1 to 1),
        listOf(-1 to 1, 0 to 0, 0 to 1, 1 to 0)
    )),
    J(listOf(
        listOf(0 to -1, 0 to 0, 0 to 1, 1 to -1),
        listOf(-1 to 0, 0 to 0, 1 to 0, 1 to 1),
        listOf(0 to -1, 0 to 0, 0 to 1, -1 to 1),
        listOf(-1 to -1, -1 to 0, 0 to 0, 1 to 0)
    )),
    L(listOf(
        listOf(0 to -1, 0 to 0, 0 to 1, 1 to 1),
        listOf(-1 to 0, 0 to 0, 1 to 0, -1 to 1),
        listOf(0 to -1, 0 to 0, 0 to 1, -1 to -1),
        listOf(1 to -1, -1 to 0, 0 to 0, 1 to 0)
    ));

    fun cellsAt(rotation: Int, origin: Pair<Int, Int>): List<Pair<Int, Int>> {
        return shape[rotation % 4].map { (dy, dx) ->
            origin.first + dy to origin.second + dx
        }
    }
}

class MoveMapper(private val spawnColumn: Int = 4) {

    fun generateInputSequence(rotation: Int, column: Int): List<Int> {
        val sequence = mutableListOf<Int>()

        repeat(rotation) {
            sequence += KeyEvent.VK_C // Rotate CW
        }

        val delta = column - 4 // assume spawn column is 4
        val key = if (delta < 0) KeyEvent.VK_LEFT else KeyEvent.VK_RIGHT
        repeat(kotlin.math.abs(delta)) {
            sequence += key
        }

        sequence += KeyEvent.VK_UP // Hard drop
        return sequence
    }

    fun execute(robot: Robot, inputs: List<Int>, delayMs: Long = 10L) {
        for (key in inputs) {
            robot.keyPress(key)
            robot.keyRelease(key)
            Thread.sleep(delayMs)
        }
    }
}
class TetrisBoard(val width: Int = 10, val height: Int = 20) {
    private val grid = Array(height) { BooleanArray(width) }

    fun isCellOccupied(row: Int, col: Int): Boolean =
        row < 0 || row >= height || col < 0 || col >= width || grid[row][col]

    fun canPlace(tetromino: Tetromino, rotation: Int, origin: Pair<Int, Int>): Boolean {
        return tetromino.cellsAt(rotation, origin).all { (r, c) -> !isCellOccupied(r, c) }
    }

    fun place(tetromino: Tetromino, rotation: Int, origin: Pair<Int, Int>) {
        tetromino.cellsAt(rotation, origin).forEach { (r, c) ->
            if (r in 0 until height && c in 0 until width) {
                grid[r][c] = true
            }
        }
    }

    fun dropPiece(tetromino: Tetromino, rotation: Int, column: Int): Boolean {
        for (row in height downTo 0) {
            val origin = row to column
            if (canPlace(tetromino, rotation, origin)) {
                place(tetromino, rotation, origin)
                clearFullLines()
                return true
            }
        }
        return false
    }

    private fun clearFullLines() {
        val newGrid = mutableListOf<BooleanArray>()
        var cleared = 0
        for (row in grid) {
            if (row.all { it }) {
                cleared++
            } else {
                newGrid.add(row)
            }
        }
        repeat(cleared) {
            newGrid.add(BooleanArray(width))
        }
        for (i in 0 until height) {
            grid[i] = newGrid[height - 1 - i]
        }
    }

    private fun cloneGrid(): Array<BooleanArray> =
        Array(height) { row -> grid[row].clone() }

    private fun evaluateBoard(tempGrid: Array<BooleanArray>): Int {
        var holes = 0
        var aggregateHeight = 0
        var linesCleared = 0
        val columnHeights = IntArray(width) { 0 }

        for (col in 0 until width) {
            var found = false
            for (row in 0 until height) {
                if (tempGrid[row][col]) {
                    if (!found) {
                        columnHeights[col] = height - row
                        aggregateHeight += height - row
                        found = true
                    }
                } else if (found) {
                    holes++
                }
            }
        }

        var bumpiness = 0
        for (i in 0 until width - 1) {
            bumpiness += kotlin.math.abs(columnHeights[i] - columnHeights[i + 1])
        }

        for (row in 0 until height) {
            if (tempGrid[row].all { it }) linesCleared++
        }

        // Heavily reward line clears, prioritize line making over stack shape
        return holes * 100 + aggregateHeight * 1 + bumpiness * 2 - linesCleared * 3000
    }

    fun autoSelect(tetromino: Tetromino): Pair<Int, Int>? {
        var bestScore = Int.MAX_VALUE
        var bestPlacement: Pair<Int, Int>? = null

        for (rotation in 0 until 4) {
            for (col in 0 until width) {
                for (row in height downTo 0) {
                    val origin = row to col
                    if (canPlace(tetromino, rotation, origin)) {
                        val tempGrid = cloneGrid()
                        tetromino.cellsAt(rotation, origin).forEach { (r, c) ->
                            if (r in 0 until height && c in 0 until width) {
                                tempGrid[r][c] = true
                            }
                        }
                        val score = evaluateBoard(tempGrid)
                        if (score < bestScore || (score == bestScore && col > (bestPlacement?.second ?: -1))) {
                            bestScore = score
                            bestPlacement = rotation to col
                        }
                        break
                    }
                }
            }
        }

        return bestPlacement
    }

    fun updateFromGameFrame(mat: Mat, origin: Point, cellSize: Size) {
        for (row in 0 until height) {
            for (col in 0 until width) {
                val x = (origin.x + col * cellSize.width).toInt()
                val y = (origin.y + row * cellSize.height).toInt()
                val region = mat.submat(Rect(x, y, cellSize.width.toInt(), cellSize.height.toInt()))
                val avg = Core.mean(region)
                grid[row][col] = avg.`val`.take(3).sum() > 100 // heuristic brightness
            }
        }
    }

    fun drawOverlay(mat: Mat, origin: Point, cellSize: Size) {
        val boardRect = Rect(
            origin.x.toInt(),
            origin.y.toInt(),
            (width * cellSize.width).toInt(),
            (height * cellSize.height).toInt()
        )
        Imgproc.rectangle(mat, boardRect, Scalar(0.0, 0.0, 255.0), 2)
    }

    fun printBoard() {
        for (row in grid.reversed()) {
            println(row.joinToString("", prefix = "|", postfix = "|") { if (it) "#" else " " })
        }
        println("+" + "-".repeat(width) + "+")
    }
}