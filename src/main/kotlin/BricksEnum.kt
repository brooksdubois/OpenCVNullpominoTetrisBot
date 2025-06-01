import org.opencv.core.Scalar

enum class Brick(ascii: List<String>, val color: Scalar) {
    T(T_ASCII, Scalar(155.4, 110.0, 151.9)),
    I(I_ASCII, Scalar(153.8, 208.7, 22.9)),
    O(O_ASCII, Scalar(22.2, 209.4, 156.8)),
    S(S_ASCII, Scalar(28.0, 219.7, 86.2)),
    Z(Z_ASCII, Scalar(29.8, 135.8, 168.8)),
    J(J_ASCII, Scalar(34.7, 184.0, 164.4)),
    L(L_ASCII, Scalar(159.2, 141.1, 46.1));

    val rotations: List<List<Pair<Int, Int>>> = ascii.map(::parseAscii)

    companion object {
        fun fromColor(color: Scalar): Brick? {
            var closest: Brick? = null
            var minDistance = Double.MAX_VALUE

            for (brick in values()) {
                val ref = brick.color
                val dr = color.`val`[2] - ref.`val`[2]
                val dg = color.`val`[1] - ref.`val`[1]
                val db = color.`val`[0] - ref.`val`[0]
                val dist = dr * dr + dg * dg + db * db

                if (dist < minDistance) {
                    minDistance = dist
                    closest = brick
                }
            }

            return closest
        }

        fun canPlaceOnGrid(
            grid: Array<BooleanArray>, brick: Brick, rotation: Int, origin: Pair<Int, Int>
        ): Boolean =
            brick.rotations[rotation % 4].all { (dy, dx) ->
                val (r, c) = origin.first + dy to origin.second + dx
                r in grid.indices && c in grid[0].indices && !grid[r][c]
            }

    }
}

fun parseAscii(ascii: String): List<Pair<Int, Int>> {
    val raw = ascii.trim().lines().flatMapIndexed { row, line ->
        line.mapIndexedNotNull { col, ch -> if (ch == '#') row to col else null }
    }
    val minRow = raw.minOfOrNull { it.first } ?: 0
    val minCol = raw.minOfOrNull { it.second } ?: 0
    return raw.map { (r, c) -> r - minRow to c - minCol }
}

fun classifyPieceColor(color: Scalar): Brick? = Brick.fromColor(color)
