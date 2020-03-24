package io.quarkus.it.resteasy.jackson;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GeneratedClassesTest {

    @Test
    void testGeneratedContextResolver() {
        assertNotNull(generatedContextResolver());
    }

    private Object generatedContextResolver() {
        try {
            Class<?> objectMapperResolverClass = Class
                    .forName("io.quarkus.resteasy.common.runtime.jackson.QuarkusObjectMapperContextResolver");
            return objectMapperResolverClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
