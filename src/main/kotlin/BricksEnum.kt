import kotlin.math.roundToInt

enum class BrickHue(val range: IntRange, val brick: Brick) {
    Z(0..15, Brick.Z),
    L(15..22, Brick.L),
    O(22..30, Brick.O),
    S(40..55, Brick.S),
    I(85..100, Brick.I),
    J(105..120, Brick.J),
    T(140..155, Brick.T);

    companion object {
        fun fromHue(hue: Double): Brick? {
            val h = hue.roundToInt()
            return entries.firstOrNull { h in it.range }?.brick
        }
    }
}

enum class Brick(ascii: List<String>) {
    T(T_ASCII),
    I(I_ASCII),
    O(O_ASCII),
    S(S_ASCII),
    Z(Z_ASCII),
    J(J_ASCII),
    L(L_ASCII);

    val rotations: List<List<Pair<Int, Int>>> = ascii.map(::parseAscii)

    companion object {
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
