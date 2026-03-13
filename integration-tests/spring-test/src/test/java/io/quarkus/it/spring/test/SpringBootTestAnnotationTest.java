package io.quarkus.it.spring.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import io.quarkus.arc.Arc;

@SpringBootTest
public class SpringBootTestAnnotationTest {

    @Test
    public void testQuarkusIsRunning() {
        assertNotNull(Arc.container(), "Quarkus container should be running");
    }
}
