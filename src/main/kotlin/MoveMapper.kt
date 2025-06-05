import java.awt.Robot
import java.awt.event.KeyEvent

class MoveMapper(
    private val delayMs: Long = 35L,
    private val rotateFirst: Boolean = true
) {
    private val spawnColumns = mapOf(
        Brick.I to 5,
        Brick.O to 4,
        Brick.T to 3,
        Brick.S to 3,
        Brick.Z to 3,
        Brick.J to 3,
        Brick.L to 3
    )

    fun spawnColumnFor(brick: Brick): Int = spawnColumns[brick] ?: 4

    fun generateInputSequence(rotation: Int, column: Int, spawnColumn: Int, brick: Brick): List<Int> = buildList {
        val delta = column - spawnColumn

        if (rotateFirst) repeat(rotation) { add(KeyEvent.VK_C) }

        val dirKey = if (delta < 0) KeyEvent.VK_LEFT else KeyEvent.VK_RIGHT
        repeat(kotlin.math.abs(delta)) { add(dirKey) }

        if (!rotateFirst) repeat(rotation) { add(KeyEvent.VK_C) }

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