package io.quarkus.smallrye.openapi.deployment;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import io.smallrye.openapi.runtime.scanner.spi.AnnotationScanner;

class CustomPathExtensionTest {

    AnnotationScanner scanner;

    @BeforeEach
    void setup() {
        scanner = Mockito.mock(AnnotationScanner.class);
    }

    @Test
    void testContextPathNotInvokedForEmptyPaths() {
        CustomPathExtension ext = new CustomPathExtension("/", "");
        ext.processScannerApplications(scanner, Collections.emptyList());
        Mockito.verify(scanner, Mockito.never()).setContextRoot(Mockito.anyString());
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
        ext.processScannerApplications(scanner, Collections.emptyList());
        Mockito.verify(scanner).setContextRoot(expected);
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
        ext.processScannerApplications(scanner, List.of(Index.of(TestApp.class).getClassByName(TestApp.class)));
        Mockito.verify(scanner, Mockito.times(times)).setContextRoot(times > 0 ? expected : Mockito.anyString());
    }

}
