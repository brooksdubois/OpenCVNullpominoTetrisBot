package tetris.opencv

import engine.Tetromino
import org.opencv.core.Scalar

private val pieceColorMap = mapOf(
    Tetromino.O to Scalar(22.2, 209.4, 156.8),
    Tetromino.S to Scalar(28.0, 219.7, 86.2),
    Tetromino.Z to Scalar(29.8, 135.8, 168.8),
    Tetromino.T to Scalar(155.4, 110.0, 151.9),
    Tetromino.J to Scalar(34.7, 184.0, 164.4),
    Tetromino.L to Scalar(159.2, 141.1, 46.1),
    Tetromino.I to Scalar(153.8, 208.7, 22.9)
)

fun classifyPieceColor(color: Scalar): Tetromino? {
    var closest: Tetromino? = null
    var minDistance = Double.MAX_VALUE

    for ((piece, refColor) in pieceColorMap) {
        val dr = color.`val`[2] - refColor.`val`[2]
        val dg = color.`val`[1] - refColor.`val`[1]
        val db = color.`val`[0] - refColor.`val`[0]
        val dist = dr * dr + dg * dg + db * db

        if (dist < minDistance) {
            minDistance = dist
            closest = piece
        }
    }

    return closest
}
