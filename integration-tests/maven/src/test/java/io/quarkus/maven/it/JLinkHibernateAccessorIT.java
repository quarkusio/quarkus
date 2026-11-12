package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.restassured.RestAssured;

@DisableForNative
@EnabledForJreRange(min = JRE.JAVA_25, disabledReason = "The jlink extension requires Java 25 or later")
class JLinkHibernateAccessorIT extends MojoTestBase {
    @ParameterizedTest
    @ValueSource(strings = { "generated", "reflection", "generated-with-fallback" })
    void accessorsRespectModuleBoundaries(String strategy) throws Exception {
        File testDir = initProject("projects/jlink-hibernate-accessor", "jlink-hibernate-accessor-" + strategy);
        RunningInvoker running = new RunningInvoker(testDir, false);
        try {
            MavenProcessInvocationResult result = running.execute(
                    List.of("install", "-DskipTests", "-Dquarkus.hibernate-accessor.strategy=" + strategy), Map.of());
            assertThat(result.getProcess().waitFor(10, TimeUnit.MINUTES)).as("Maven build timed out").isTrue();
            assertThat(result.getProcess().exitValue()).withFailMessage(running.log()).isZero();
        } finally {
            running.stop();
        }
        Path image = testDir.toPath().resolve("target/jlink-output/image");
        assertThat(image).isDirectory();
        String launcher = OS.current() == OS.WINDOWS ? "accessor-test.bat" : "accessor-test";
        Path log = testDir.toPath().resolve("app/target/accessor-output.log");
        Process process = new ProcessBuilder(image.resolve("bin").resolve(launcher).toAbsolutePath().toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try (var cleanup = cleanup(process)) {
            try {
                await().atMost(TestUtils.getDefaultTimeout(), TimeUnit.MINUTES)
                        .failFast(() -> !process.isAlive()).ignoreExceptions()
                        .untilAsserted(() -> assertThat(RestAssured.get("/accessors/ready").asString()).isEqualTo("ready"));
                var response = RestAssured.get("/accessors");
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.asString()).isEqualTo(strategy + ":ok");
            } catch (Exception | AssertionError failure) {
                throw new AssertionError("Modular accessor test failed for " + strategy + "\n" + readLog(log), failure);
            }
        }
    }

    // Try-with-resources suppresses cleanup failures onto the assertion containing the application log.
    static AutoCloseable cleanup(Process process) {
        return () -> {
            // Keep the handles: a shell launcher can exit before its child JVM does.
            List<ProcessHandle> children = process.descendants().toList();
            try {
                children.forEach(ProcessHandle::destroy);
                process.destroy();
                process.waitFor(10, TimeUnit.SECONDS);
                for (ProcessHandle child : children) {
                    if (child.isAlive()) {
                        child.onExit().get(10, TimeUnit.SECONDS);
                    }
                }
            } catch (TimeoutException ignored) {
                // Force any remaining children below, even if the launcher has already exited.
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } finally {
                children.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        };
    }

    private static String readLog(Path log) {
        try {
            return Files.readString(log);
        } catch (Exception e) {
            return "Could not read " + log + ": " + e;
        }
    }
}
