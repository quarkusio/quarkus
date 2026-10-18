package io.quarkus.smallrye.openapi.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CustomPathExtensionTest {

    @Test
    void testContextPathNotInvokedForEmptyPaths() {
        CustomPathExtension ext = new CustomPathExtension("/", "");
        String contextRoot = ext.resolveContextRoot(Collections.emptyList());
        assertNull(contextRoot);
    }

    @ParameterizedTest
    @CsvSource({
            "'/root', 'app-from-config'  , '/root/app-from-config'",
            "'/'    , '/app-from-config/', '/app-from-config'",
            "''     , '/app-from-config/', '/app-from-config'",
            "       , 'app-from-config'  , '/app-from-config'",
    })
    void testContextPathGenerationWithoutApplicationPathAnnotation(String rootPath, String appPath, String expected) {
        CustomPathExtension ext = new CustomPathExtension(rootPath, appPath);
        String contextRoot = ext.resolveContextRoot(Collections.emptyList());
        assertEquals(expected, contextRoot);
    }

    @ParameterizedTest
    @CsvSource({
            "'/root', 'app-from-config'  , 1, '/root'",
            "'/'    , '/app-from-config/', 0, ",
            "''     , '/app-from-config/', 0, ",
            "       , 'app-from-config'  , 0, ",
    })
    void testContextPathGenerationWithApplicationPathAnnotation(String rootPath, String appPath, int times, String expected)
            throws IOException {
        @jakarta.ws.rs.ApplicationPath("app-path-from-anno")
        class TestApp extends jakarta.ws.rs.core.Application {
        }

        CustomPathExtension ext = new CustomPathExtension(rootPath, appPath);
        String contextRoot = ext.resolveContextRoot(List.of(Index.of(TestApp.class).getClassByName(TestApp.class)));
        assertEquals(expected, contextRoot);
    }

}
