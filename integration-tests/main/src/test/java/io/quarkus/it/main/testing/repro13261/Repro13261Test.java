package io.quarkus.it.main.testing.repro13261;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.test.junit.QuarkusTest;

@Disabled("https://github.com/quarkusio/quarkus/issues/13261")
// fails with `expected: not <null>`
@QuarkusTest
public class Repro13261Test {
    @TempDir
    Path tempDir;

    @Test
    public void test() {
        assertNotNull(tempDir);
    }
}
