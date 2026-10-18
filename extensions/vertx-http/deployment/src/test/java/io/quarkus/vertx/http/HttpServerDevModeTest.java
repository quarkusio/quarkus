package io.quarkus.vertx.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.TestResourceScope;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.vertx.http.HttpServerDevModeTest.ServerResource;
import io.smallrye.config.Config;

@WithTestResource(value = ServerResource.class, scope = TestResourceScope.MATCHING_RESOURCES)
public class HttpServerDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @TestHTTPResource
    URI uri;

    HttpServer httpServer;
    ValueRegistry valueRegistry;
    Config config;

    @Test
    void ports(HttpServer httpServer) {
        assertNotNull(httpServer);
        assertEquals(8080, httpServer.getPort());
        assertNotNull(uri);
        assertEquals(httpServer.getLocalBaseUri(), uri);
        assertEquals(8080, this.httpServer.getPort());
        assertEquals(8080, this.httpServer.getLocalBaseUri().getPort());
    }

    @Test
    void valueRegistry(ValueRegistry valueRegistry) {
        assertEquals(8080, valueRegistry.get(HttpServer.HTTP_PORT));
        assertEquals(8080, valueRegistry.get(HttpServer.LOCAL_BASE_URI).getPort());
        assertEquals(8080, this.valueRegistry.get(HttpServer.HTTP_PORT));
        assertEquals(8080, this.valueRegistry.get(HttpServer.LOCAL_BASE_URI).getPort());
    }

    @Test
    void config(Config config) {
        assertEquals(8080, config.getValue("quarkus.http.port", int.class));
        assertEquals(8080, this.config.getValue("quarkus.http.port", int.class));
    }

    @Test
    @Disabled("""
            Throws a CCE if the resource is Global. If local, it fails because TestResourceManager is cached by the first \
            test that executes (which might be a different one) and doesn't pick up the local test resource.
            """)
    void serverResource(Config config) {
        assertEquals(9999, config.getValue("server.port", int.class));
        assertEquals(9999, Config.get().getValue("server.port", int.class));
    }

    public static class ServerResource implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            return Map.of("server.port", "9999");
        }

        @Override
        public void stop() {

        }
    }
}
