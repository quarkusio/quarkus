package io.quarkus.it.spring.test;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpringBootTestOriginalAnnotationTest {

    @Test
    public void testQuarkusIsRunning() {
        fail("Test should not be invoked");
    }
}
