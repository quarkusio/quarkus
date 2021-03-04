package io.quarkus.micrometer.deployment.binder;

import java.util.Map;
import java.util.regex.Pattern;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpServerMetrics;

public class VertxWithHttpEnabledTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.ignore-patterns", "/http")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.match-patterns", "/one=/two")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.ignore-patterns", "/two")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Instance<VertxMeterBinderAdapter> vertxMeterBinderAdapterInstance;

    @Inject
    HttpBinderConfiguration httpBinderConfiguration;

    @Test
    public void testMetricFactoryCreatedMetrics() throws Exception {
        Assertions.assertTrue(vertxMeterBinderAdapterInstance.isResolvable());
        VertxMeterBinderAdapter adapter = vertxMeterBinderAdapterInstance.get();

        HttpServerMetrics metrics = adapter.createHttpServerMetrics(new HttpServerOptions(), new SocketAddress() {
            @Override
            public String host() {
                return "a.b.c";
            }

            @Override
            public int port() {
                return 0;
            }

            @Override
            public String path() {
                return null;
            }

            @Override
            public String hostName() {
                return null;
            }

            @Override
            public String hostAddress() {
                return null;
            }

            @Override
            public boolean isInetSocket() {
                return false;
            }

            @Override
            public boolean isDomainSocket() {
                return false;
            }
        });

        Assertions.assertNotNull(metrics);
        Assertions.assertTrue(httpBinderConfiguration.isServerEnabled());

        // prefer http-server.ignore-patterns
        Assertions.assertEquals(1, httpBinderConfiguration.getServerIgnorePatterns().size());
        Pattern p = httpBinderConfiguration.getServerIgnorePatterns().get(0);
        Assertions.assertTrue(p.matcher("/http").matches());

        // Use vertx.match-patterns (http-server version is missing)
        Assertions.assertEquals(1, httpBinderConfiguration.getServerMatchPatterns().size());
        Map.Entry<Pattern, String> entry = httpBinderConfiguration.getServerMatchPatterns().entrySet().iterator().next();
        Assertions.assertTrue(entry.getKey().matcher("/one").matches());
        Assertions.assertEquals("/two", entry.getValue());
    }
}
