package io.quarkus.it.vertx;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.SmallRyeConfig;

@QuarkusTest
public class VerticleTest {

    protected Path getLogPath() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        LogRuntimeConfig logRuntimeConfig = config.getConfigMapping(LogRuntimeConfig.class);
        return logRuntimeConfig.file().path().toPath();
    }

    @Test
    public void testBareVerticle() {
        String s = get("/vertx-test/verticles/bare").asString();
        assertThat(s).isEqualTo("OK-bare");
    }

    @Test
    public void testBareWithClassNameVerticle() {
        String s = get("/vertx-test/verticles/bare-classname").asString();
        assertThat(s).isEqualTo("OK-bare-classname");
    }

    @Test
    public void testMdcVerticle() {
        Path logFilePath = getLogPath();
        String value = UUID.randomUUID().toString();
        given().queryParam("value", value)
                .get("/vertx-test/verticles/mdc")
                .then()
                .body(is("OK-" + value));
        Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        assertTrue(Files.exists(logFilePath),
                                "quarkus log file " + logFilePath + " is missing");
                        String data = Files.readString(logFilePath);
                        String receivedMessage = "Received message ### " + value;
                        String timerFired = "Timer fired ### " + value;
                        String blockingTask = "Blocking task executed ### " + value;
                        assertTrue(data.contains(receivedMessage),
                                "log doesn't contain: " + receivedMessage);
                        assertTrue(data.contains(timerFired),
                                "log doesn't contain: " + timerFired);
                        assertTrue(data.contains(blockingTask),
                                "log doesn't contain: " + blockingTask);
                    }
                });
    }
}
