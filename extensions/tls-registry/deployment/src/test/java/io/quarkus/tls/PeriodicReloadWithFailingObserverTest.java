package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

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

/**
 * A failing observer stops the remaining ones, so the rotation must be notified again at the next period instead of
 * being lost.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-failing-observer-A", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:localhost"),
        @Certificate(name = "test-failing-observer-B", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:acme.org")
})
public class PeriodicReloadWithFailingObserverTest {

    private static final String configuration = """
            # No config
            """;

    public static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FailingOnceObserver.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.path", temp.getAbsolutePath() + "/tls.p12")
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.password", "password")
            .overrideRuntimeConfigKey("quarkus.tls.reload-period", "1s")
            .overrideRuntimeConfigKey("loc", temp.getAbsolutePath())
            .setBeforeAllCustomizer(() -> {
                try {
                    temp.mkdirs();
                    Files.copy(new File("target/certs/test-failing-observer-A-keystore.p12").toPath(),
                            new File(temp, "/tls.p12").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @Inject
    FailingOnceObserver observer;

    @ConfigProperty(name = "loc")
    File certs;

    @Test
    void testRotationIsNotifiedAgainAfterAFailingObserver() throws Exception {
        File tmp = new File(certs, "/tls.p12.tmp");
        Files.copy(new File("target/certs/test-failing-observer-B-keystore.p12").toPath(), tmp.toPath());
        Files.move(tmp.toPath(), new File(certs, "/tls.p12").toPath(), StandardCopyOption.REPLACE_EXISTING);

        // The first notification fails, the next period notifies again and succeeds
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(observer.events()).hasSize(2));
        // Once delivered, the rotation is not notified again
        await().during(3, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS)
                .until(() -> observer.events().size() == 2);
    }

    @ApplicationScoped
    public static class FailingOnceObserver {

        private final CopyOnWriteArrayList<CertificateUpdatedEvent> receivedEvents = new CopyOnWriteArrayList<>();

        public void onCertificateUpdate(@Observes CertificateUpdatedEvent event) {
            receivedEvents.add(event);
            if (receivedEvents.size() == 1) {
                throw new IllegalStateException("boom");
            }
        }

        public CopyOnWriteArrayList<CertificateUpdatedEvent> events() {
            return receivedEvents;
        }
    }
}
