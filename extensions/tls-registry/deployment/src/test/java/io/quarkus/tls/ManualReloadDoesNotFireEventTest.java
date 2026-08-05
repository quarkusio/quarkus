package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-reload-event-A", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:localhost"),
        @Certificate(name = "test-reload-event-B", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:acme.org")
})
public class ManualReloadDoesNotFireEventTest {

    private static final String configuration = """
            # No config
            """;

    public static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(CertificateUpdateObserver.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.path", temp.getAbsolutePath() + "/tls.p12")
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.password", "password")
            .overrideRuntimeConfigKey("loc", temp.getAbsolutePath())
            .setBeforeAllCustomizer(() -> {
                try {
                    temp.mkdirs();
                    Files.copy(new File("target/certs/test-reload-event-A-keystore.p12").toPath(),
                            new File(temp, "/tls.p12").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @Inject
    TlsConfigurationRegistry registry;

    @Inject
    CertificateUpdateObserver observer;

    @ConfigProperty(name = "loc")
    File certs;

    @Test
    void testManualReloadDoesNotFireEvent() throws IOException {
        TlsConfiguration def = registry.getDefault().orElseThrow();
        assertThat(observer.events()).isEmpty();

        Files.copy(new File("target/certs/test-reload-event-B-keystore.p12").toPath(),
                new File(certs, "/tls.p12").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertThat(def.reload()).isTrue();
        assertThat(observer.events()).isEmpty();
    }

    @ApplicationScoped
    public static class CertificateUpdateObserver {

        private final CopyOnWriteArrayList<CertificateUpdatedEvent> receivedEvents = new CopyOnWriteArrayList<>();

        public void onCertificateUpdate(@Observes CertificateUpdatedEvent event) {
            receivedEvents.add(event);
        }

        public CopyOnWriteArrayList<CertificateUpdatedEvent> events() {
            return receivedEvents;
        }
    }
}
