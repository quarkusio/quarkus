package org.jboss.resteasy.reactive.common.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;

public class UriBuilderImplTest {

    @Path("/resource")
    public static class SampleResource {

        @Path("/get")
        public String getIt() {
            return null;
        }

        public String noPath() {
            return null;
        }

        @Path("/dup")
        public String duplicate() {
            return null;
        }

        @Path("/dup2")
        public String duplicate(String arg) {
            return null;
        }
    }

    @Test
    void resolvesMethodAnnotatedWithPath() {
        assertEquals("/get", new UriBuilderImpl().path(SampleResource.class, "getIt").toTemplate());
    }

    @Test
    void repeatedCallsAreConsistent() {
        assertEquals("/get", new UriBuilderImpl().path(SampleResource.class, "getIt").toTemplate());
        assertEquals("/get", new UriBuilderImpl().path(SampleResource.class, "getIt").toTemplate());
    }

    @Test
    void throwsWhenNoMethodAnnotatedWithPath() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new UriBuilderImpl().path(SampleResource.class, "noPath"));
        assertTrue(ex.getMessage().contains("No public method annotated with @Path"));
    }

    @Test
    void throwsWhenTwoMethodsWithSameNameAnnotatedWithPath() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new UriBuilderImpl().path(SampleResource.class, "duplicate"));
        assertTrue(ex.getMessage().contains("Two methods with the same path"));
    }

    @Test
    void throwsWhenMethodDoesNotExist() {
        assertThrows(IllegalArgumentException.class,
                () -> new UriBuilderImpl().path(SampleResource.class, "missing"));
    }

    @Test
    void throwsOnNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new UriBuilderImpl().path((Class) null, "x"));
        assertThrows(IllegalArgumentException.class, () -> new UriBuilderImpl().path(SampleResource.class, (String) null));
    }

    @Test
    void usesRecordedPathWhenRegistryHasEntry() {
        try {
            // a value different from the reflectively resolved "/get" proves the registry is consulted first
            ResourceMethodPathRegistry.setResourceMethodPaths(
                    Map.of(SampleResource.class.getName(), Map.of("getIt", "/recorded")));
            assertEquals("/recorded", new UriBuilderImpl().path(SampleResource.class, "getIt").toTemplate());
        } finally {
            ResourceMethodPathRegistry.clear();
        }
    }

    @Test
    void fallsBackToReflectionWhenRegistryHasNoEntryForClass() {
        try {
            ResourceMethodPathRegistry.setResourceMethodPaths(Map.of("some.other.Class", Map.of("x", "/y")));
            assertEquals("/get", new UriBuilderImpl().path(SampleResource.class, "getIt").toTemplate());
        } finally {
            ResourceMethodPathRegistry.clear();
        }
    }

    @Test
    void clearRevertsToReflection() {
        ResourceMethodPathRegistry.setResourceMethodPaths(
                Map.of(SampleResource.class.getName(), Map.of("getIt", "/recorded")));
        ResourceMethodPathRegistry.clear();
        assertEquals("/get", new UriBuilderImpl().path(SampleResource.class, "getIt").toTemplate());
    }
}
