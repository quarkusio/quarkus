package io.quarkus.it.spring.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.test.spring.SpringBootTest;

@SpringBootTest
public class QuarkusSpringBootTestAnnotationTest {

    @Test
    public void testQuarkusIsRunning() {
        assertNotNull(Arc.container(), "Quarkus container should be running");
    }
}
