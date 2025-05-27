package input

import java.awt.Robot
import java.awt.event.KeyEvent
import kotlin.math.abs

class MoveMapper(private val spawnColumn: Int = 4) {

    fun generateInputSequence(rotation: Int, column: Int): List<Int> {
        val sequence = buildList {
            repeat(rotation) { add(KeyEvent.VK_SHIFT) }
            val delta = column - spawnColumn
            val directionKey = if (delta < 0) KeyEvent.VK_LEFT else KeyEvent.VK_RIGHT
            repeat(abs(delta)) { add(directionKey) }
            add(KeyEvent.VK_UP) // Hard drop
        }
        return sequence
    }

    fun execute(robot: Robot, inputs: List<Int>, delayMs: Long = 30L) {
        for (key in inputs) {
            robot.keyPress(key)
            robot.keyRelease(key)
            Thread.sleep(delayMs)
        }
    }
}