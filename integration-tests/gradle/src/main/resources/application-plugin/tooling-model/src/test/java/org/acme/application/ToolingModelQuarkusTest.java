package org.acme.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ToolingModelQuarkusTest {

    @Test
    void bootstrapsApplicationAndWorkspaceDependency() {
        assertEquals("library", ApplicationValue.value());
    }
}
