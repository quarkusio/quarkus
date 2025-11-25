package io.quarkus.it.spring.web;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

//import org.springframework.boot.test.context.SpringBootTest;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SpringBootExampleTest {

    @Test
    public void testQuarkusIsRunning() {
        assertNotNull(Arc.container(), "Quarkus container should be running");
    }
}
