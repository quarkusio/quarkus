package io.quarkus.opentelemetry.observation.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.observation.ObservationRegistry;
import io.quarkus.test.QuarkusExtensionTest;

public class DisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addAsResource(new StringAsset(
                                    "quarkus.otel.observation.enabled=false\n"),
                                    "application.properties"));

    @Inject
    Instance<ObservationRegistry> registry;

    @Test
    void registryNotAvailableWhenDisabled() {
        assertThat(registry.isResolvable()).isFalse();
    }
}
