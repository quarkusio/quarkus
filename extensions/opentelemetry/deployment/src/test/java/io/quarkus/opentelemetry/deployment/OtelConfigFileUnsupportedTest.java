package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Setting the {@code otel.config.file} system property fails startup, as Quarkus does not support file-based
 * configuration.
 */
public class OtelConfigFileUnsupportedTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(SdkAtStartup.class))
            // autoconfigure read otel.config.file from system properties and environment variables only
            .setBeforeAllCustomizer(new Runnable() {
                @Override
                public void run() {
                    System.setProperty("otel.config.file", "otel-config.yaml");
                }
            })
            .setAfterAllCustomizer(new Runnable() {
                @Override
                public void run() {
                    System.clearProperty("otel.config.file");
                }
            })
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .assertException(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    Throwable cause = throwable;
                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    // The application class loader loads the exception, so the class is compared by name
                    assertThat(cause.getClass().getName()).isEqualTo(ConfigurationException.class.getName());
                    assertThat(cause)
                            .hasMessage("otel.config.file is not supported; configure the SDK with quarkus.otel.* properties");
                }
            });

    @Test
    void test() {
    }

    // Builds the SDK at startup, as an application that uses it does
    @Singleton
    public static class SdkAtStartup {

        void onStart(@Observes StartupEvent event, OpenTelemetry openTelemetry) {
        }
    }
}
