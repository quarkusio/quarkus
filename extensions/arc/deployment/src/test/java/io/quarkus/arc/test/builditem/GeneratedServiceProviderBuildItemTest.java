package io.quarkus.arc.test.builditem;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.ServiceLoader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ComponentsProvider;
import io.quarkus.test.QuarkusExtensionTest;

public class GeneratedServiceProviderBuildItemTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(SimpleBean.class));

    @Inject
    SimpleBean bean;

    @Test
    void serviceProviderFileIsPresentOnClasspath() {
        URL resource = Thread.currentThread().getContextClassLoader()
                .getResource("META-INF/services/io.quarkus.arc.ComponentsProvider");
        assertNotNull(resource, "META-INF/services/io.quarkus.arc.ComponentsProvider must be on the classpath");
    }

    @Test
    void serviceLoaderFindsComponentsProvider() {
        assertTrue(ServiceLoader.load(ComponentsProvider.class).iterator().hasNext(),
                "ServiceLoader must find at least one ComponentsProvider implementation");
    }

    @ApplicationScoped
    static class SimpleBean {
        String ping() {
            return "pong";
        }
    }
}
