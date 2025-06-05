import TetrisMoves.maxStackHeight
import TetrisMoves.dropPieceOnGrid
import kotlin.math.max

fun evaluateLikeBasicAI(
    originalGrid: Array<BooleanArray>,
    newGrid: Array<BooleanArray>,
    linesCleared: Int,
    tspin: Boolean = false,
    combo: Int = 0,
    allClear: Boolean = false
): Int {
    var pts = 0
    val heightBefore = maxStackHeight(originalGrid)
    val heightAfter = maxStackHeight(newGrid)
    val holesBefore = countHoles(originalGrid)
    val holesAfter = countHoles(newGrid)
    val lidsBefore = countLids(originalGrid)
    val lidsAfter = countLids(newGrid)

    // Major bonuses
    if (allClear) pts += 500_000
    if (tspin && linesCleared > 0) pts += 100_000 * linesCleared

    // Line clears
    if (!tspin && !allClear) {
        pts += when (linesCleared) {
            1 -> 10
            2 -> 50
            3 -> 100
            4 -> 100_000
            else -> 0
        }
    }

    // Hole penalty / reward
    pts -= max(0, holesAfter - holesBefore) * 10
    pts += max(0, holesBefore - holesAfter) * 5

    // Lid penalty / reward
    pts -= max(0, lidsAfter - lidsBefore) * 10
    pts += max(0, lidsBefore - lidsAfter) * 10

    // Height improvement bonus
    pts += (heightBefore - heightAfter) * 10

    // Combo bonus
    if (linesCleared > 0) {
        pts += linesCleared * combo * 100
    }

    return pts
}

fun countHoles(grid: Array<BooleanArray>): Int {
    val width = grid[0].size
    val height = grid.size
    var holes = 0
    for (col in 0 until width) {
        var foundBlock = false
        for (row in 0 until height) {
            if (grid[row][col]) foundBlock = true
            else if (foundBlock) holes++
        }
    }
    return holes
}

fun countLids(grid: Array<BooleanArray>): Int {
    val width = grid[0].size
    val height = grid.size
    var lids = 0
    for (col in 0 until width) {
        var holeSeen = false
        for (row in 0 until height - 1) {
            if (!grid[row][col] && grid[row + 1][col]) holeSeen = true
            if (holeSeen && grid[row][col]) lids++
        }
    }
    return lids
}

fun autoSelectWithLookaheadBasicAI(
    board: TetrisBoard,
    queue: List<Brick>,
    depth: Int = 1
): Pair<Int, Int>? {
    if (queue.isEmpty()) return null

    var bestScore = Int.MIN_VALUE
    var bestMove: Pair<Int, Int>? = null
    val grid = board.getGrid()
    val height = board.height
    val width = board.width
    val currentPiece = queue[0]

    for (rotation in 0 until 4) {
        for (col in 0 until width) {
            val dropRow = (height downTo 0).firstOrNull { row ->
                Brick.canPlaceOnGrid(grid, currentPiece, rotation, row to col) &&
                        board.hasVerticalAccess(currentPiece, rotation, row to col)
            } ?: continue

            val newGrid = dropPieceOnGrid(grid, currentPiece, rotation, col) ?: continue
            val linesCleared = newGrid.count { it.all { it } }
            val isAllClear = newGrid.all { row -> row.none { it } }
            val score = evaluateLikeBasicAI(grid, newGrid, linesCleared, tspin = false, combo = 0, allClear = isAllClear)

            if (score > bestScore) {
                bestScore = score
                bestMove = rotation to col
            }
        }
    }

    return bestMove
}