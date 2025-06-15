package io.quarkus.vertx.mdc;

import static io.quarkus.vertx.mdc.VerticleDeployer.REQUEST_ID_HEADER;
import static io.quarkus.vertx.mdc.VerticleDeployer.VERTICLE_PORT;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.test.QuarkusDevModeTest;

public class VertxMDCDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest().withApplicationRoot((jar) -> jar
            .addClasses(JavaArchive.class, VerticleDeployer.class, InMemoryLogHandler.class,
                    InMemoryLogHandlerProducer.class)
            .add(new StringAsset("quarkus.log.file.enable=true\n"
                    + "quarkus.log.console.format=%d{HH:mm:ss} %-5p requestId=%X{requestId} [%c{2.}] (%t) %s%e%n\n"),
                    "application.properties"))
            .setLogFileName("quarkus-mdc.log");

    @Test
    void mdcDevMode() {
        InMemoryLogHandler.reset();
        await().until(InitialConfigurator.DELAYED_HANDLER::isActivated);
        await().until(() -> {
            return Arrays.stream(InitialConfigurator.DELAYED_HANDLER.getHandlers())
                    .anyMatch(h -> h.getClass().getName().contains("InMemoryLogHandler"));
        });
        ;

        Path logDirectory = Paths.get(".", "target");
        String value = UUID.randomUUID().toString();
        given().headers(REQUEST_ID_HEADER, value).get("http://localhost:" + VERTICLE_PORT + "/").then().body(is(value));
        Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS).atMost(2, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        final Path logFilePath = logDirectory.resolve("quarkus-mdc.log");
                        assertTrue(Files.exists(logFilePath), "quarkus log file " + logFilePath + " is missing");
                        String data = Files.readString(logFilePath);
                        String receivedMessage = "Received HTTP request ### " + value;
                        String timerFired = "Timer fired ### " + value;
                        String blockingTask = "Blocking task executed ### " + value;
                        String webClientResponse = "Received Web Client response ### " + value;
                        assertTrue(data.contains(receivedMessage), "log doesn't contain: " + receivedMessage);
                        assertTrue(data.contains(timerFired), "log doesn't contain: " + timerFired);
                        assertTrue(data.contains(blockingTask), "log doesn't contain: " + blockingTask);
                        assertTrue(data.contains(webClientResponse), "log doesn't contain: " + webClientResponse);
                    }
                });
    }
}
