package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStore;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.deployment.spi.TlsCertificateBuildItem;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-registration", password = "password", formats = Format.PKCS12)
})
public class BuildTimeRegistrationTest {

    private static final String configuration = """
            # no configuration by default
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());;

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testBuildTimeRegistration() {
        TlsConfiguration conf = registry.get("named").orElseThrow();
        assertThat(conf.getKeyStore()).isNotNull();
        assertThat(conf.getTrustStore()).isNotNull();
    }

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        TlsCertificateBuildItem item = new TlsCertificateBuildItem("named", new MyCertificateSupplier());
                        context.produce(item);
                    }
                })
                        .produces(TlsCertificateBuildItem.class)
                        .build();
            }
        };
    }

    public static class MyCertificateSupplier implements Supplier<TlsConfiguration> {

        @Override
        public TlsConfiguration get() {
            try {
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(getClass().getResourceAsStream("target/certs/test-registration-keystore.p12"),
                        "password".toCharArray());
                KeyStore ts = KeyStore.getInstance("PKCS12");
                ts.load(getClass().getResourceAsStream("target/certs/test-registration-truststore.p12"),
                        "password".toCharArray());
                return new BaseTlsConfiguration() {
                    @Override
                    public KeyStore getKeyStore() {
                        return ks;
                    }

                    @Override
                    public KeyStore getTrustStore() {
                        return ts;
                    }

                    @Override
                    public boolean isTrustAll() {
                        return false;
                    }

                    @Override
                    public String getName() {
                        return "test";
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
