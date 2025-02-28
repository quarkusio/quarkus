package io.quarkus.it.main.testing.repro8446;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class Repro8446Test {
    @TestTemplate
    @ExtendWith(GreeterExtension.class)
    public void test(Greeter greeter) {
        assertEquals("hello", greeter.hello());
    }
}
