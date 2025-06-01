import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt

enum class CellState { EMPTY, GHOST, REAL }



object TetrisOverlay {

    // Helper to convert CellState grid to Boolean grid for AI logic
    fun cellStateGridToBool(grid: Array<Array<CellState>>): Array<BooleanArray> =
        Array(grid.size) { row -> BooleanArray(grid[0].size) { col -> grid[row][col] == CellState.REAL } }


    fun extractCellStatesFromFrame(
        mat: Mat, origin: Point, cellSize: Size, width: Int, height: Int
    ): Array<Array<CellState>> {
        val grid = Array(height) { Array(width) { CellState.EMPTY } }
        for (row in 0 until height) {
            for (col in 0 until width) {
                val cellX = origin.x + col * cellSize.width
                val cellY = origin.y + row * cellSize.height
                val regionSize = 4
                val centerX = cellX + cellSize.width / 2
                val centerY = cellY + cellSize.height / 2
                val half = regionSize / 2
                val regionX = (centerX - half).roundToInt().coerceIn(0, mat.cols() - regionSize)
                val regionY = (centerY - half).roundToInt().coerceIn(0, mat.rows() - regionSize)
                val region = mat.submat(Rect(regionX, regionY, regionSize, regionSize))
                val avgBGRScalar = org.opencv.core.Core.mean(region)
                val avgBGR = doubleArrayOf(
                    avgBGRScalar.`val`[0],
                    avgBGRScalar.`val`[1],
                    avgBGRScalar.`val`[2]
                )
                val (_, _, v) = avgBGR.toHSV()
                grid[row][col] = when {
                    v > 180 -> CellState.REAL
                    v > 100 -> CellState.GHOST
                    else -> CellState.EMPTY
                }
            }
        }
        return grid
    }

    fun drawOverlay(mat: Mat, origin: Point, cellSize: Size, width: Int, height: Int) {
        val boardRect = Rect(
            origin.x.toInt(),
            origin.y.toInt(),
            (width * cellSize.width).toInt(),
            (height * cellSize.height).toInt()
        )
        Imgproc.rectangle(mat, boardRect, Scalar(0.0, 0.0, 255.0), 2)
    }

    fun drawBotPrediction(
        mat: Mat, origin: Point, cellSize: Size,
        brick: Brick, rotation: Int, column: Int,
        grid: Array<BooleanArray>
    ) {
        val height = grid.size
        val width = grid[0].size

        val shape = brick.rotations[rotation % 4]

        // Calculate left-most dx to align anchor correctly
        val minDx = shape.minOf { it.second }

        val dropRow = (height downTo 0).firstOrNull { row ->
            Brick.canPlaceOnGrid(grid, brick, rotation, row to (column - minDx)) &&
                    TetrisMoves.hasVerticalAccessStatic(grid, brick, rotation, row to (column - minDx))
        } ?: return

        // Align to true top-left corner of the shape
        for ((dy, dx) in shape) {
            val r = dropRow + dy
            val c = column - minDx + dx
            if (r in 0 until height && c in 0 until width) {
                val x = origin.x + c * cellSize.width
                val y = origin.y + r * cellSize.height
                Imgproc.rectangle(
                    mat,
                    Point(x, y),
                    Point(x + cellSize.width, y + cellSize.height),
                    Scalar(255.0, 0.0, 255.0), 2
                )
            }
        }
    }

    fun drawCellOverlays(
        mat: Mat,
        origin: Point,
        cellSize: Size,
        cellStates: Array<Array<CellState>>
    ) {
        val height = cellStates.size
        val width = cellStates[0].size
        for (row in 0 until height) {
            for (col in 0 until width) {
                val x = origin.x + col * cellSize.width
                val y = origin.y + row * cellSize.height
                val color = when (cellStates[row][col]) {
                    CellState.REAL -> Scalar(0.0, 255.0, 0.0)    // Green
                    CellState.GHOST -> Scalar(0.0, 255.0, 255.0) // Yellow
                    CellState.EMPTY -> Scalar(0.0, 0.0, 0.0)     // Black
                }
                Imgproc.rectangle(
                    mat,
                    Point(x, y),
                    Point(x + cellSize.width, y + cellSize.height),
                    color,
                    -1
                )
            }
        }
    }
}
