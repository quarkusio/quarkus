package io.quarkus.vertx.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.value.registry.ValueRegistry;

public class HttpServerDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @TestHTTPResource
    URI uri;

    ValueRegistry valueRegistry;

    @Test
    void ports(HttpServer httpServer) {
        assertNotNull(httpServer);
        assertEquals(8080, httpServer.getPort());
        assertNotNull(uri);
        assertEquals(httpServer.getLocalBaseUri(), uri);

        assertNotNull(valueRegistry);
    }
}
