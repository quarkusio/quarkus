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
            Class<?> jsonbResolverClass = Class
                    .forName("io.quarkus.resteasy.jackson.runtime.QuarkusObjectMapperContextResolver");
            return jsonbResolverClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
