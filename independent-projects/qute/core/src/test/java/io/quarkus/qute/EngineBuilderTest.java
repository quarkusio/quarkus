package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class EngineBuilderTest {

    @Test
    public void testDuplicateNamespace() {
        try {
            Engine.builder().addNamespaceResolver(NamespaceResolver.builder("foo").resolve(e -> {
                return null;
            }).build()).addNamespaceResolver(NamespaceResolver.builder("foo").resolve(e -> {
                return null;
            }).build());
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

}
