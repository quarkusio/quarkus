package io.quarkus.stork.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Formatter;
import java.util.logging.Level;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.stork.SmallRyeStorkRegistrationRecorder;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Metadata.DefaultMetadataKey;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.spi.internal.ServiceRegistrarLoader;

public class RegistrationTimeoutTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(UnresponsiveServiceRegistrarLoader.class)
                    .addAsServiceProvider(ServiceRegistrarLoader.class, UnresponsiveServiceRegistrarLoader.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.smallrye.stork", "stork-service-registration-static-list", storkVersion())))
            .overrideConfigKey("quarkus.stork.my-service.service-registrar.type", UnresponsiveServiceRegistrarLoader.TYPE)
            .overrideConfigKey("quarkus.stork.other-service.service-registrar.type", "static")
            .overrideConfigKey("quarkus.stork.registration-timeout", "500ms")
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && SmallRyeStorkRegistrationRecorder.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records)
                    .singleElement()
                    .satisfies(record -> assertThat(LOG_FORMATTER.formatMessage(record))
                            .isEqualTo("Failed to complete registration for service 'my-service' within 500 ms."
                                    + " The timeout can be configured with 'quarkus.stork.registration-timeout'")));

    @Test
    public void shouldStartWhenRegistrationTimesOut() {
        assertThat(UnresponsiveServiceRegistrarLoader.REGISTRATIONS).hasValue(1);
    }

    private static String storkVersion() {
        try (InputStream is = RegistrationTimeoutTest.class
                .getResourceAsStream("/META-INF/maven/io.smallrye.stork/stork-core/pom.properties")) {
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Provides a service registrar that completes neither the registration nor the deregistration.
     */
    public static class UnresponsiveServiceRegistrarLoader implements ServiceRegistrarLoader<DefaultMetadataKey> {

        static final String TYPE = "unresponsive";
        static final AtomicInteger REGISTRATIONS = new AtomicInteger();

        @Override
        public ServiceRegistrar<DefaultMetadataKey> createServiceRegistrar(ConfigWithType config, String serviceName,
                StorkInfrastructure storkInfrastructure) {
            return new ServiceRegistrar<>() {
                @Override
                public Uni<Void> registerServiceInstance(String serviceName, String instanceName,
                        Metadata<DefaultMetadataKey> metadata, String ipAddress, int defaultPort) {
                    REGISTRATIONS.incrementAndGet();
                    return Uni.createFrom().nothing();
                }

                @Override
                public Uni<Void> deregisterServiceInstance(String serviceName) {
                    return Uni.createFrom().nothing();
                }
            };
        }

        @Override
        public String type() {
            return TYPE;
        }
    }
}
