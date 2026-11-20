package io.quarkus.smallrye.openapi.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.EmptyIndex;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CustomPathExtensionTest {

    @Test
    void testContextPathNotInvokedForEmptyPaths() {
        CustomPathExtension ext = new CustomPathExtension("/", "", EmptyIndex.INSTANCE);
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
        CustomPathExtension ext = new CustomPathExtension(rootPath, appPath, EmptyIndex.INSTANCE);
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

        Index index = Index.of(TestApp.class);
        CustomPathExtension ext = new CustomPathExtension(rootPath, appPath, index);
        String contextRoot = ext.resolveContextRoot(List.of(index.getClassByName(TestApp.class)));
        assertEquals(expected, contextRoot);
    }

    @ParameterizedTest
    @CsvSource({
            "'/root', 'app-from-config'  , '/root/app-from-config'",
            "'/'    , '/app-from-config/', '/app-from-config'",
    })
    void testContextPathKeepsAppPathForApplicationSubclass(String rootPath, String appPath, String expected)
            throws IOException {
        class BaseApp extends jakarta.ws.rs.core.Application {
        }

        class TestApp extends BaseApp {
        }

        Index index = Index.of(BaseApp.class, TestApp.class);
        CustomPathExtension ext = new CustomPathExtension(rootPath, appPath, index);
        assertEquals(expected, ext.resolveContextRoot(List.of(index.getClassByName(TestApp.class))));
    }

    @ParameterizedTest
    @CsvSource({
            "'/root', 'app-from-config'  , '/root/app-from-config'",
            "'/'    , '/app-from-config/', '/app-from-config'",
    })
    void testContextPathKeepsAppPathForSpringControllers(String rootPath, String appPath, String expected)
            throws IOException {
        @org.springframework.web.bind.annotation.RestController
        class TestController {
        }

        @org.springframework.web.bind.annotation.RequestMapping("/mapped")
        class TestMapping {
        }

        Index index = Index.of(TestController.class, TestMapping.class);
        CustomPathExtension ext = new CustomPathExtension(rootPath, appPath, index);
        assertEquals(expected, ext.resolveContextRoot(List.of(index.getClassByName(TestController.class))));
        assertEquals(expected, ext.resolveContextRoot(List.of(index.getClassByName(TestMapping.class))));
    }

    @ParameterizedTest
    @CsvSource({
            "'/root', 'app-from-config'  , '/root'",
            "'/'    , '/app-from-config/', ",
            "''     , 'app-from-config'  , ",
    })
    void testContextPathIgnoresAppPathForClassesNotServedByRest(String rootPath, String appPath, String expected)
            throws IOException {
        class TestRoute {
            @io.quarkus.vertx.web.Route(path = "/hello")
            void hello() {
            }
        }

        @io.quarkus.vertx.web.RouteBase(path = "base")
        class TestRouteBase {
        }

        class SomethingElse {
        }

        Index index = Index.of(TestRoute.class, TestRouteBase.class, SomethingElse.class);
        CustomPathExtension ext = new CustomPathExtension(rootPath, appPath, index);
        assertEquals(expected, ext.resolveContextRoot(List.of(index.getClassByName(TestRoute.class))));
        assertEquals(expected, ext.resolveContextRoot(List.of(index.getClassByName(TestRouteBase.class))));
        assertEquals(expected, ext.resolveContextRoot(List.of(index.getClassByName(SomethingElse.class))));
    }

}
