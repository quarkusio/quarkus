package io.quarkus.it.camel.jdbc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CamelJdbcTest {

    @Test
    public void selectToFile() throws Throwable {
        final long timeoutMs = 10000;
        final long deadline = System.currentTimeMillis() + timeoutMs;
        final Path outCsv = Paths.get("target/out.txt");
        Throwable lastException = null;
        final String expectedCsv = "[{SPECIES=Camelus dromedarius}]";
        while (System.currentTimeMillis() <= deadline) {
            try {
                Thread.sleep(100);
                if (!Files.exists(outCsv)) {
                    lastException = new AssertionError(String.format("%s does not exist", outCsv));
                } else {
                    final String actual = new String(Files.readAllBytes(outCsv), StandardCharsets.UTF_8);
                    if (expectedCsv.equals(actual)) {
                        /* Test passed */
                        return;
                    } else {
                        lastException = new AssertionError(String.format("expected: <%s> but was: <%s>", expectedCsv, actual));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw lastException;
    }
}
