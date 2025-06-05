
class TetrisBoard(val width: Int = 10, val height: Int = 20) {
    private val grid = Array(height) { BooleanArray(width) }

    fun isCellOccupied(row: Int, col: Int) =
        row !in 0 until height || col !in 0 until width || grid[row][col]

//    fun canPlace(brick: Brick, rotation: Int, origin: Pair<Int, Int>) =
//        Brick.canPlaceOnGrid(grid, brick, rotation, origin)

    fun hasVerticalAccess(brick: Brick, rotation: Int, origin: Pair<Int, Int>): Boolean {
        return brick.rotations[rotation % 4].all { (dy, dx) ->
            val col = origin.second + dx
            val top = origin.first + dy
            (0 until top).all { row -> !isCellOccupied(row, col) }
        }
    }


//    fun place(brick: Brick, rotation: Int, origin: Pair<Int, Int>) {
//        brick.rotations[rotation % 4].forEach { (dy, dx) ->
//            val r = origin.first + dy
//            val c = origin.second + dx
//            if (r in 0 until height && c in 0 until width) grid[r][c] = true
//        }
//    }
//
//    fun clearFullLines() {
//        val newGrid = mutableListOf<BooleanArray>()
//        var cleared = 0
//        for (row in grid) {
//            if (row.all { it }) cleared++
//            else newGrid.add(row)
//        }
//        repeat(cleared) { newGrid.add(BooleanArray(width)) }
//        for (i in 0 until height) grid[i] = newGrid[height - 1 - i]
//    }

    fun cloneGrid() = Array(height) { grid[it].clone() }

    fun updateFromGameFrame(cells: Array<BooleanArray>) {
        for (row in 0 until height)
            for (col in 0 until width)
                grid[row][col] = cells[row][col]
    }

    fun getGrid(): Array<BooleanArray> = grid.map { it.clone() }.toTypedArray()
}
