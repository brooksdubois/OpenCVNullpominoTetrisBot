package ai

import engine.TetrisBoard
import engine.Tetromino
import input.MoveMapper
import java.awt.Robot

class TurnExecutor(
    private val robot: Robot,
    private val moveMapper: MoveMapper = MoveMapper()
) {
    fun playTurn(board: TetrisBoard, tetromino: Tetromino) {
        val best = board.autoSelect(tetromino)
        if (best != null) {
            val (rotation, column) = best
            println("→ Placing $tetromino at col $column, rot $rotation")
            val inputs = moveMapper.generateInputSequence(rotation, column)
            moveMapper.execute(robot, inputs)
            board.dropPiece(tetromino, rotation, column)
            Thread.sleep(500)
        } else {
            println("× No valid placement found for $tetromino")
        }
    }
}