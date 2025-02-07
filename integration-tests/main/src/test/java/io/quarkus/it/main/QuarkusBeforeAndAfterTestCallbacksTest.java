package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusBeforeAndAfterTestCallbacksTest {

    @BeforeEach
    @AfterEach
    public void ensureMissingSystemProperty() {
        assertNull(System.getProperty("quarkus.test.method"));
    }

    @Test
    public void actualTest() {
        assertEquals("actualTest", System.getProperty("quarkus.test.method"));
    }
}
