package ilove.quark.us

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import io.quarkus.test.junit.main.Launch
import io.quarkus.test.junit.main.LaunchResult
import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest

@QuarkusMainTest
class GreetingCommandTest {

    @Test
    fun testBasicLaunch(launcher: QuarkusMainLauncher) {
        val result = launcher.launch()
        assertTrue(result.output.contains("Hello picocli, go go commando!"), result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Launch(["Alice"])
    fun testLaunchWithArguments(result: LaunchResult) {
        assertTrue(result.output.contains("Hello Alice, go go commando!"), result.output)
    }
}