package engine

import opencv.toHSV
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.roundToInt

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
            if (canPlace(tetromino, rotation, origin) && hasVerticalAccess(tetromino, rotation, origin)) {
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
            if (row.all { it }) cleared++
            else newGrid.add(row)
        }
        repeat(cleared) { newGrid.add(BooleanArray(width)) }
        for (i in 0 until height) {
            grid[i] = newGrid[height - 1 - i]
        }
    }

    private fun cloneGrid(): Array<BooleanArray> =
        Array(height) { row -> grid[row].clone() }

    private fun evaluateBoard(tempGrid: Array<BooleanArray>): Int {
        var holes = 0
        var existingHoleFills = 0
        var aggregateHeight = 0
        var linesCleared = 0
        val columnHeights = IntArray(width)

        for (col in 0 until width) {
            var foundBlock = false
            for (row in 0 until height) {
                if (tempGrid[row][col]) {
                    if (!foundBlock) {
                        columnHeights[col] = height - row
                        aggregateHeight += height - row
                        foundBlock = true
                    }
                } else if (foundBlock) {
                    holes++
                    if (grid[row][col]) existingHoleFills++ // filled a previous hole
                }
            }
        }

        for (row in 0 until height) {
            if (tempGrid[row].all { it }) linesCleared++
        }

        var bumpiness = 0
        for (i in 0 until width - 1) {
            bumpiness += abs(columnHeights[i] - columnHeights[i + 1])
        }

        return linesCleared * 1500 +  // high reward for clearing
                existingHoleFills * 200 -  // reward filling prior holes
                holes * 150 -              // penalize new holes
                bumpiness * 5 -
                aggregateHeight
    }

    fun updateFromGameFrame(mat: Mat, origin: Point, cellSize: Size) {
        for (row in 0 until height) {
            for (col in 0 until width) {
                val cellX = origin.x + col * cellSize.width
                val cellY = origin.y + row * cellSize.height

                // Define 4×4 subregion near the center
                val regionSize = 4
                val centerX = cellX + cellSize.width / 2
                val centerY = cellY + cellSize.height / 2
                val half = regionSize / 2

                val regionX = (centerX - half).roundToInt().coerceIn(0, mat.cols() - regionSize)
                val regionY = (centerY - half).roundToInt().coerceIn(0, mat.rows() - regionSize)

                val region = mat.submat(Rect(regionX, regionY, regionSize, regionSize))
                val avgBGRScalar = Core.mean(region)
                val avgBGR = doubleArrayOf(
                    avgBGRScalar.`val`[0],
                    avgBGRScalar.`val`[1],
                    avgBGRScalar.`val`[2]
                )

                val (h, s, v) = avgBGR.toHSV()
                val isReal = v > 180
                grid[row][col] = isReal

                // Visual debug overlay (preserve full precision)
                val overlayColor = when {
                    v > 180 -> Scalar(0.0, 255.0, 0.0)     // Green = real
                    v > 100 -> Scalar(0.0, 255.0, 255.0)    // Yellow = ghost
                    else -> Scalar(0.0, 0.0, 0.0)           // Black = empty
                }

                val drawX = cellX.roundToInt()
                val drawY = cellY.roundToInt()

                Imgproc.rectangle(
                    mat,
                    Point(drawX.toDouble(), drawY.toDouble()),
                    Point((drawX + cellSize.width).toInt().toDouble(), (drawY + cellSize.height).toInt().toDouble()),
                    overlayColor,
                    -1
                )

                Imgproc.putText(
                    mat,
                    "%.0f".format(v),
                    Point(drawX + 2.0, drawY + cellSize.height - 2.0),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.3,
                    Scalar(255.0, 255.0, 255.0),
                    1
                )
            }
        }
    }

    fun clone(): TetrisBoard {
        val cloned = TetrisBoard(width, height)
        for (row in 0 until height) {
            cloned.grid[row] = grid[row].clone()
        }
        return cloned
    }

    fun monteCarloSelect(current: Tetromino, generatePiece: () -> Tetromino, rollouts: Int = 50, depth: Int = 4): Pair<Int, Int> {
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int> = 0 to 0

        for (rotation in 0 until 4) {
            for (col in 0 until width) {
                var finalRow = -1
                for (row in height downTo 0) {
                    if (canPlace(current, rotation, row to col)) {
                        finalRow = row
                        break
                    }
                }

                if (finalRow == -1) continue
                place(current, rotation, finalRow to col)
                clearFullLines()

                var totalScore = 0
                repeat(rollouts) {
                    val simBoard = clone()
                    repeat(depth) {
                        val next = generatePiece()
                        val move = simBoard.autoSelect(current, next)
                        simBoard.dropPiece(next, move.first, move.second)
                    }
                    totalScore += evaluateBoard(cloneGrid())
                }

                val avg = totalScore / rollouts
                if (avg > bestScore) {
                    bestScore = avg
                    bestMove = rotation to col
                }
            }
        }

        return bestMove
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

    fun simulateNextPiece(grid: Array<BooleanArray>, tetromino: Tetromino): Int {
        var best = Int.MIN_VALUE
        for (rotation in 0 until 4) {
            for (col in 0 until width) {
                for (row in height downTo 0) {
                    val origin = row to col
                    if (!canPlace(tetromino, rotation, origin, grid)) continue
                    val temp = grid.map { it.clone() }.toTypedArray()
                    tetromino.cellsAt(rotation, origin).forEach { (r, c) ->
                        if (r in 0 until height && c in 0 until width)
                            temp[r][c] = true
                    }
                    val score = evaluateBoard(temp)
                    if (score > best) best = score
                }
            }
        }
        return best
    }

    fun canPlace(tetromino: Tetromino, rotation: Int, origin: Pair<Int, Int>, grid: Array<BooleanArray>): Boolean {
        return tetromino.cellsAt(rotation, origin).all { (r, c) ->
            r in 0 until height && c in 0 until width && !grid[r][c]
        }
    }

    fun autoSelect(tetromino: Tetromino, next: Tetromino?): Pair<Int, Int> {
        var bestScore = Int.MIN_VALUE
        var bestPlacement: Pair<Int, Int> = 0 to 0

        for (rotation in 0 until 4) {
            for (col in 0 until width) {
                for (row in height downTo 0) {
                    val origin = row to col
                    if (!canPlace(tetromino, rotation, origin) || !hasVerticalAccess(tetromino, rotation, origin)) continue

                    val tempGrid = cloneGrid()
                    tetromino.cellsAt(rotation, origin).forEach { (r, c) ->
                        if (r in 0 until height && c in 0 until width)
                            tempGrid[r][c] = true
                    }

                    val baseScore = evaluateBoard(tempGrid)

                    // 🔁 Lookahead: simulate next piece
                    val lookaheadScore = next?.let {
                        simulateNextPiece(tempGrid, it)
                    } ?: 0

                    val combinedScore = baseScore + lookaheadScore

                    if (combinedScore > bestScore) {
                        bestScore = combinedScore
                        bestPlacement = rotation to col
                    }
                }
            }
        }

        return bestPlacement
    }


    fun drawBotPrediction(
        mat: Mat,
        origin: Point,
        cellSize: Size,
        tetromino: Tetromino,
        rotation: Int,
        column: Int
    ) {
        for (row in height downTo 0) {
            val originCell = row to column
            if (canPlace(tetromino, rotation, originCell)) {
                val blocks = tetromino.cellsAt(rotation, originCell)
                for ((r, c) in blocks) {
                    if (r in 0 until height && c in 0 until width) {
                        val x = origin.x + c * cellSize.width
                        val y = origin.y + r * cellSize.height
                        Imgproc.rectangle(
                            mat,
                            Point(x, y),
                            Point(x + cellSize.width, y + cellSize.height),
                            Scalar(255.0, 0.0, 255.0), // pink outline
                            2
                        )
                    }
                }
                break
            }
        }
    }

    private fun hasVerticalAccess(tetromino: Tetromino, rotation: Int, origin: Pair<Int, Int>): Boolean {
        val cells = tetromino.cellsAt(rotation, origin)
        val columns = cells.map { it.second }.distinct()

        for (col in columns) {
            val maxRow = cells.filter { it.second == col }.minOf { it.first }
            for (row in 0 until maxRow) {
                if (isCellOccupied(row, col)) return false
            }
        }
        return true
    }
}