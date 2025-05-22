package engine

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

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
        val columnHeights = IntArray(width)

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
            bumpiness += abs(columnHeights[i] - columnHeights[i + 1])
        }

        for (row in 0 until height) {
            if (tempGrid[row].all { it }) linesCleared++
        }

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
                val luma = 0.299 * avg.`val`[2] + 0.587 * avg.`val`[1] + 0.114 * avg.`val`[0]
                grid[row][col] = luma > 60 // 🔧 tweak this threshold if needed
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

    fun drawDebugOverlay(mat: Mat, origin: Point, cellSize: Size) {
        for (row in 0 until height) {
            for (col in 0 until width) {
                val x = (origin.x + col * cellSize.width).toInt()
                val y = (origin.y + row * cellSize.height).toInt()
                val region = mat.submat(Rect(x, y, cellSize.width.toInt(), cellSize.height.toInt()))
                val avg = Core.mean(region)
                val luma = 0.299 * avg.`val`[2] + 0.587 * avg.`val`[1] + 0.114 * avg.`val`[0]

                // Colored overlay
                val overlayColor = if (luma > 60) Scalar(0.0, 255.0, 0.0) else Scalar(0.0, 0.0, 0.0)
                Imgproc.rectangle(
                    mat,
                    Point(x.toDouble(), y.toDouble()),
                    Point(x + cellSize.width, y + cellSize.height),
                    overlayColor,
                    -1
                )

                // Luma label
                Imgproc.putText(
                    mat,
                    "%.0f".format(luma),
                    Point(x + 2.0, y + cellSize.height - 2.0),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.3,
                    Scalar(255.0, 255.0, 255.0),
                    1
                )
            }
        }
    }
}
