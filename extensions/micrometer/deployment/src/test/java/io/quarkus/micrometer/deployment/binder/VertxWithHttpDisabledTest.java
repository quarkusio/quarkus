package io.quarkus.micrometer.deployment.binder;

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

public class VertxWithHttpDisabledTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Instance<VertxMeterBinderAdapter> vertxMeterBinderAdapterInstance;

    @Inject
    HttpBinderConfiguration httpBinderConfiguration;

    @Test
    public void testVertxMetricsWithoutHttp() throws Exception {
        Assertions.assertTrue(vertxMeterBinderAdapterInstance.isResolvable());
        VertxMeterBinderAdapter adapter = vertxMeterBinderAdapterInstance.get();

        // HttpServerMetrics should not be created (null returned) because
        // Http server metrics are disabled
        Assertions.assertNull(adapter.createHttpServerMetrics(new HttpServerOptions(), new SocketAddress() {
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
        }));

        Assertions.assertFalse(httpBinderConfiguration.isServerEnabled());
    }
}
