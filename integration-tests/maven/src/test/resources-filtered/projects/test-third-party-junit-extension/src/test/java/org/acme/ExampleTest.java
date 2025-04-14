package org.acme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

@ExtendWith(CustomBeforeAllCallback.class)
@QuarkusTest
class ExampleTest {
    @Test
    void testSomething() {
    }

}
