import java.awt.Robot
import java.awt.event.KeyEvent

class MoveMapper {
    fun spawnColumnFor(brick: Brick): Int =
        when (brick) {
            Brick.I -> 3
            else -> 4
        }

    fun generateInputSequence(rotation: Int, column: Int, spawnColumn: Int): List<Int>
        = buildList {
            repeat(rotation) { add(KeyEvent.VK_SHIFT) }
            val delta = column - spawnColumn
            val directionKey = if (delta < 0) KeyEvent.VK_LEFT else KeyEvent.VK_RIGHT
            repeat(kotlin.math.abs(delta)) { add(directionKey) }
            add(KeyEvent.VK_UP) // Hard drop
        }

    fun execute(robot: Robot, inputs: List<Int>, delayMs: Long = 30L) {
        for (key in inputs) {
            robot.keyPress(key)
            robot.keyRelease(key)
            Thread.sleep(delayMs)
        }
    }
}
