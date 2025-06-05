import java.awt.Robot
import java.awt.event.KeyEvent

class MoveMapper(
    private val delayMs: Long = 35L,
    private val rotateFirst: Boolean = true
) {
    private val spawnColumns = mapOf(
        Brick.I to 3,
        Brick.O to 4,
        Brick.T to 3,
        Brick.S to 3,
        Brick.Z to 3,
        Brick.J to 3,
        Brick.L to 3
    )

    fun spawnColumnFor(brick: Brick): Int = spawnColumns[brick] ?: 4

    private fun anchorColumn(brick: Brick, rotation: Int): Int =
        brick.rotations[rotation % 4].minOf { it.second }

    fun generateInputSequence(rotation: Int, column: Int, spawnColumn: Int, brick: Brick): List<Int> = buildList {
        val startOffset = anchorColumn(brick, 0)
        val endOffset = anchorColumn(brick, rotation % 4)
        val adjustedSpawnCol = spawnColumn - startOffset
        val adjustedTargetCol = column - endOffset
        val delta = adjustedTargetCol - adjustedSpawnCol

        if (rotateFirst) repeat(rotation) { add(KeyEvent.VK_SHIFT) }

        val dirKey = if (delta < 0) KeyEvent.VK_LEFT else KeyEvent.VK_RIGHT
        repeat(kotlin.math.abs(delta)) { add(dirKey) }

        if (!rotateFirst) repeat(rotation) { add(KeyEvent.VK_SHIFT) }

        add(KeyEvent.VK_UP)
    }

    fun execute(robot: Robot, inputs: List<Int>) {
        println("🕹 Executing input sequence: ${inputs.map { KeyEvent.getKeyText(it) }}")
        for (key in inputs) {
            robot.keyPress(key)
            robot.keyRelease(key)
            Thread.sleep(delayMs)
        }
    }
}