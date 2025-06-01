import kotlin.math.abs
const val MAX_ALLOWED_HEIGHT = 10

object TetrisMoves {
    fun maxStackHeight(grid: Array<BooleanArray>): Int {
        for (row in grid.indices) {
            if (grid[row].any { it }) {
                return grid.size - row
            }
        }
        return 0
    }


    fun dropPieceOnGrid(
        grid: Array<BooleanArray>, brick: Brick, rotation: Int, column: Int
    ): Array<BooleanArray>? {
        val height = grid.size
        val width = grid[0].size
        val dropRow = (height downTo 0).firstOrNull { row ->
            Brick.canPlaceOnGrid(grid, brick, rotation, row to column) &&
            hasVerticalAccessStatic(grid, brick, rotation, row to column)
        } ?: return null

        val newGrid = Array(height) { grid[it].clone() }
        brick.rotations[rotation % 4].forEach { (dy, dx) ->
            val r = dropRow + dy
            val c = column + dx
            if (r in 0 until height && c in 0 until width) newGrid[r][c] = true
        }
        return newGrid
    }

    fun hasVerticalAccessStatic(
        grid: Array<BooleanArray>, brick: Brick, rotation: Int, origin: Pair<Int, Int>
    ): Boolean {
        val cells = brick.rotations[rotation % 4].map { (dy, dx) -> origin.first + dy to origin.second + dx }
        return cells.groupBy { it.second }.all { (_, colCells) ->
            val topRow = colCells.minOf { it.first }
            (0 until topRow).all { row ->
                row in grid.indices && colCells.first().second in grid[0].indices && !grid[row][colCells.first().second]
            }
        }
    }

    fun simulateLookahead(
        grid: Array<BooleanArray>, queue: List<Brick>, depth: Int,
        evaluate: (Array<BooleanArray>) -> Int
    ): Int {
        if (depth == 0 || queue.isEmpty()) return evaluate(grid)
        val height = grid.size
        val width = grid[0].size

        return (0 until 4).flatMap { rotation ->
            (0 until width).mapNotNull { col ->
                val dropRow = (height downTo 0).firstOrNull { row ->
                    Brick.canPlaceOnGrid(grid, queue[0], rotation, row to col) &&
                    hasVerticalAccessStatic(grid, queue[0], rotation, row to col)
                } ?: return@mapNotNull null
                val newGrid = dropPieceOnGrid(grid, queue[0], rotation, col) ?: return@mapNotNull null
                simulateLookahead(newGrid, queue.drop(1), depth - 1, evaluate)
            }
        }.minOrNull() ?: Int.MAX_VALUE
    }

    fun autoSelectWithLookahead(
        board: TetrisBoard,
        queue: List<Brick>,
        depth: Int = 3,
        evaluate: (Array<BooleanArray>) -> Int
    ): Pair<Int, Int>? {
        if (queue.isEmpty()) return null
        var bestScore = Int.MAX_VALUE
        var bestMove: Pair<Int, Int>? = null
        val grid = board.getGrid()
        val height = board.height
        val width = board.width

        for (rotation in 0 until 4) {
            for (col in 0 until width) {
                for (row in height downTo 0) {
                    val origin = row to col
                    if (!Brick.canPlaceOnGrid(grid, queue[0], rotation, origin)) continue
                    if (!board.hasVerticalAccess(queue[0], rotation, origin)) continue

                    val tempGrid = board.cloneGrid()
                    queue[0].rotations[rotation % 4].forEach { (dy, dx) ->
                        val r = row + dy
                        val c = col + dx
                        if (r in 0 until height && c in 0 until width) tempGrid[r][c] = true
                    }
                    val score = simulateLookahead(tempGrid, queue.drop(1), depth - 1, evaluate)
                    if (score < bestScore || (score == bestScore && col > (bestMove?.second ?: -1))) {
                        bestScore = score
                        bestMove = rotation to col
                    }
                    break
                }
            }
        }
        return bestMove
    }

    fun evaluateBoardStatic(grid: Array<BooleanArray>): Int {
        val height = grid.size
        val width = grid[0].size
        var holes = 0
        var linesCleared = 0
        val columnHeights = IntArray(width)

        for (col in 0 until width) {
            var foundBlock = false
            for (row in 0 until height) {
                if (grid[row][col]) {
                    if (!foundBlock) {
                        columnHeights[col] = height - row
                        foundBlock = true
                    }
                } else if (foundBlock) {
                    holes++
                }
            }
        }

        for (row in 0 until height) {
            if (grid[row].all { it }) linesCleared++
        }

        val aggregateHeight = columnHeights.sum()
        val bumpiness = (0 until width - 1).sumOf { abs(columnHeights[it] - columnHeights[it + 1]) }
        val maxHeight = columnHeights.maxOrNull() ?: 0
        val heightPenalty = if (maxHeight > MAX_ALLOWED_HEIGHT) (maxHeight - MAX_ALLOWED_HEIGHT) * 1000 else 0

        return linesCleared * 2000 -
                holes * 500 -
                bumpiness * 10 -
                aggregateHeight * 1 -
                heightPenalty
    }
}
