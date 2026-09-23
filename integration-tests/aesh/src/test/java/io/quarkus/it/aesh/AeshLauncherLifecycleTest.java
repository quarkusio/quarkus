package io.quarkus.it.aesh;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.aesh.AeshLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * Verifies that a REPL session does not leak threads: after
 * {@link AeshLauncher#exit()} every thread the session created
 * (REPL thread, stream reader, pipe stages) must terminate.
 * <p>
 * Leaked threads accumulate across tests in the same forked JVM until
 * it wedges (later tests time out waiting for signals, and the fork
 * eventually OOMs), so this test guards the whole suite.
 */
@QuarkusMainTest
public class AeshLauncherLifecycleTest {

    @Test
    void replThreadsTerminateAfterClose(AeshLauncher launcher) {
        Map<Thread, Boolean> baseline = liveAeshThreads();

        launcher.execute("hello --name=Lifecycle");
        assertThat(launcher.getCommandOutput()).isEqualTo("Hello Lifecycle!\n");

        launcher.exit();
        assertThat(launcher.waitForExit(Duration.ofSeconds(15)))
                .as("REPL should exit")
                .isTrue();

        // Give daemon threads a grace period to unwind, then require that no
        // thread created by this session is still alive. Threads that predate
        // this test (leaked by earlier tests) are excluded via the baseline.
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        List<String> remaining;
        do {
            remaining = new ArrayList<>();
            for (Map.Entry<Thread, Boolean> entry : liveAeshThreads().entrySet()) {
                if (!baseline.containsKey(entry.getKey())) {
                    Thread thread = entry.getKey();
                    remaining.add(thread.getName() + ":" + thread.getState());
                }
            }
            if (remaining.isEmpty()) {
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        } while (System.nanoTime() < deadline);

        assertThat(remaining)
                .as("Threads leaked by the REPL session")
                .isEmpty();
    }

    /**
     * Returns the currently live threads owned by aesh REPL sessions:
     * the launcher thread, the stream reader, and upstream pipe stages.
     */
    private static Map<Thread, Boolean> liveAeshThreads() {
        Map<Thread, Boolean> result = new IdentityHashMap<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            String name = thread.getName();
            if (thread.isAlive()
                    && (name.startsWith("aesh-test-") || name.startsWith("aesh-pipe-"))) {
                result.put(thread, Boolean.TRUE);
            }
        }
        return result;
    }
}
