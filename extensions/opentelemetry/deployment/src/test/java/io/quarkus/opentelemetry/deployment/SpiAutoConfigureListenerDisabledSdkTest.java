package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With the SDK disabled, autoconfigure still configured the resource, so a {@link ResourceProvider} ran and, when it
 * implements {@link AutoConfigureListener}, received the disabled SDK.
 */
public class SpiAutoConfigureListenerDisabledSdkTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ListeningResourceProvider.class)
                            .addAsResource(new StringAsset(ListeningResourceProvider.class.getName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider"))
            .overrideConfigKey("quarkus.otel.sdk.disabled", "true")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void resourceProviderRunsAndReceivesTheSdk() {
        // forces the synthetic OpenTelemetry bean
        assertThat(openTelemetry.getTracer("test")).isNotNull();

        assertThat(ListeningResourceProvider.CREATED.get()).isTrue();
        assertThat(ListeningResourceProvider.SDK.get()).isNotNull();
        assertThat(ListeningResourceProvider.SDK.get().getPropagators()).isSameAs(openTelemetry.getPropagators());
    }

    public static class ListeningResourceProvider implements ResourceProvider, AutoConfigureListener {

        static final AtomicReference<Boolean> CREATED = new AtomicReference<>(false);
        static final AtomicReference<OpenTelemetrySdk> SDK = new AtomicReference<>();

        @Override
        public Resource createResource(ConfigProperties config) {
            CREATED.set(true);
            return Resource.empty();
        }

        @Override
        public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            SDK.set(sdk);
        }
    }
}
