package io.quarkus.it.jackson;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.LogFile;
import io.quarkus.test.QuarkusProdModeTest;

public class JacksonWithShutdownTimeoutPMT {

    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ExitResource.class))
            .setRun(true)
            .setApplicationName("jackson-shutdown-test")
            .setLogFileName("jackson-shutdown-test.log")
            .overrideConfigKey("quarkus.shutdown.timeout", "5s");

    @LogFile
    private Path logfile;

    @Test
    void test() throws IOException, InterruptedException {
        when()
                .post("/exit")
                .then()
                .statusCode(204);

        await().until(() -> Files.readString(logfile).contains("stopped in"));

        Assertions.assertThat(Files.readString(logfile)).doesNotContain("Timed out waiting for graceful shutdown");
    }
}
