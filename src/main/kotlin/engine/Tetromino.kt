package engine
enum class Tetromino(val shape: List<List<Pair<Int, Int>>>) {
    I(listOf(
        listOf(0 to -1, 0 to 0, 0 to 1, 0 to 2),
        listOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0),
        listOf(0 to -1, 0 to 0, 0 to 1, 0 to 2),
        listOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0)
    )),
    O(listOf(
        listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
        listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
        listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
        listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
    )),
    T(listOf(
        listOf(0 to -1, 0 to 0, 0 to 1, 1 to 0),
        listOf(-1 to 0, 0 to 0, 1 to 0, 0 to -1),
        listOf(0 to -1, 0 to 0, 0 to 1, -1 to 0),
        listOf(-1 to 0, 0 to 0, 1 to 0, 0 to 1)
    )),
    S(listOf(
        listOf(0 to 0, 0 to 1, 1 to -1, 1 to 0),
        listOf(-1 to 0, 0 to 0, 0 to 1, 1 to 1),
        listOf(0 to 0, 0 to 1, 1 to -1, 1 to 0),
        listOf(-1 to 0, 0 to 0, 0 to 1, 1 to 1)
    )),
    Z(listOf(
        listOf(0 to -1, 0 to 0, 1 to 0, 1 to 1),
        listOf(-1 to 1, 0 to 0, 0 to 1, 1 to 0),
        listOf(0 to -1, 0 to 0, 1 to 0, 1 to 1),
        listOf(-1 to 1, 0 to 0, 0 to 1, 1 to 0)
    )),
    J(listOf(
        listOf(0 to -1, 0 to 0, 0 to 1, 1 to -1),
        listOf(-1 to 0, 0 to 0, 1 to 0, 1 to 1),
        listOf(0 to -1, 0 to 0, 0 to 1, -1 to 1),
        listOf(-1 to -1, -1 to 0, 0 to 0, 1 to 0)
    )),
    L(listOf(
        listOf(0 to -1, 0 to 0, 0 to 1, 1 to 1),
        listOf(-1 to 0, 0 to 0, 1 to 0, -1 to 1),
        listOf(0 to -1, 0 to 0, 0 to 1, -1 to -1),
        listOf(1 to -1, -1 to 0, 0 to 0, 1 to 0)
    ));

    fun cellsAt(rotation: Int, origin: Pair<Int, Int>): List<Pair<Int, Int>> {
        return shape[rotation % 4].map { (dy, dx) ->
            origin.first + dy to origin.second + dx
        }
    }
}
