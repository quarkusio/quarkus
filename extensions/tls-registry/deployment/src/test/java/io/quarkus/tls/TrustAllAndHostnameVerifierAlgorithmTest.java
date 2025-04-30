package io.quarkus.tls;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.runtime.TrustAllOptions;

public class TrustAllAndHostnameVerifierAlgorithmTest {

    private static final String configuration = """
            quarkus.tls.trust-all=false

            quarkus.tls.open.trust-all=true
            quarkus.tls.open.hostname-verification-algorithm=NONE

            quarkus.tls.closed.trust-all=false
            quarkus.tls.closed.hostname-verification-algorithm=HTTPS
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() {
        TlsConfiguration def = certificates.getDefault().orElseThrow();
        TlsConfiguration open = certificates.get("open").orElseThrow();
        TlsConfiguration closed = certificates.get("closed").orElseThrow();

        Assertions.assertThat(def.isTrustAll()).isFalse();
        Assertions.assertThat(def.getHostnameVerificationAlgorithm()).isEmpty();

        Assertions.assertThat(open.isTrustAll()).isTrue();
        Assertions.assertThat(open.getTrustStore()).isNull();
        Assertions.assertThat(open.getTrustStoreOptions()).isEqualTo(TrustAllOptions.INSTANCE);
        Assertions.assertThat(open.getHostnameVerificationAlgorithm()).hasValue("NONE");

        Assertions.assertThat(closed.isTrustAll()).isFalse();
        Assertions.assertThat(closed.getTrustStore()).isNull();
        Assertions.assertThat(closed.getTrustStoreOptions()).isNull();
        Assertions.assertThat(closed.getHostnameVerificationAlgorithm()).hasValue("HTTPS");
    }

}
