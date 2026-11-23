package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.net.KeyStoreOptions;

/**
 * A {@link KeyStoreFactory} may return options that do not embed the store content (PKCS#11, path-based), so they
 * stay equal across a rotation. The rotation must still be notified.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-contentless-A", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:localhost"),
        @Certificate(name = "test-contentless-B", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:acme.org")
})
public class PeriodicReloadWithContentlessOptionsTest {

    private static final String configuration = """
            quarkus.tls.key-store.other.type=test-contentless
            quarkus.tls.reload-period=1s
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ContentlessKeyStoreFactory.class, CertificateUpdateObserver.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    CertificateUpdateObserver observer;

    @Test
    void testRotationIsNotifiedWhenOptionsAreUnchanged() {
        await().during(2, TimeUnit.SECONDS).atMost(4, TimeUnit.SECONDS)
                .until(() -> observer.events().isEmpty());

        ContentlessKeyStoreFactory.useCertB = true;

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(observer.events()).hasSize(1));
    }

    @ApplicationScoped
    @Identifier("test-contentless")
    public static class ContentlessKeyStoreFactory implements KeyStoreFactory {

        static volatile boolean useCertB = false;

        @Override
        public KeyStoreAndKeyCertOptions createKeyStore(OtherKeyStoreConfiguration config, Vertx vertx, String name) {
            String path = "target/certs/test-contentless-" + (useCertB ? "B" : "A") + "-keystore.p12";
            try (var fis = new FileInputStream(path)) {
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(fis, "password".toCharArray());
                // Same options whatever the content, like a PKCS#11 token
                return new KeyStoreAndKeyCertOptions(ks, new KeyStoreOptions().setType("PKCS12").setPassword("password"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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
