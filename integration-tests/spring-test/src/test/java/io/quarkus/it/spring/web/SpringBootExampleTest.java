package io.quarkus.it.spring.web;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import io.quarkus.arc.Arc;

@SpringBootTest(properties = { "mi.propiedad.test=valor123" })
public class SpringBootExampleTest {

    @Test
    public void testQuarkusIsRunning() {
        // Verifica que Quarkus está corriendo
        assertNotNull(Arc.container(), "Quarkus container should be running");

        // Verifica que la propiedad de @SpringBootTest se aplicó
        String value = System.getProperty("mi.propiedad.test");
        assertEquals("valor123", value, "Property from @SpringBootTest should be set");

        System.out.println("✅ Test ejecutado correctamente con Quarkus!");
    }
}
