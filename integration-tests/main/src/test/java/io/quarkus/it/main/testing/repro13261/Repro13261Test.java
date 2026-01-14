package io.quarkus.it.main.testing.repro13261;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class Repro13261Test {
    @TempDir
    Path tempDir;

    @Test
    public void test() {
        assertNotNull(tempDir);
    }
}
