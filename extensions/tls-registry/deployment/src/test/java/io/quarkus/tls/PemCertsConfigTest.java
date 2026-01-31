package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.tls.runtime.config.PemCertsConfig;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * Tests {@link PemCertsConfig} default methods.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "pem-certs-config-test-1", password = "password", formats = Format.PEM),
        @Certificate(name = "pem-certs-config-test-2", password = "password", formats = Format.PEM)
})
class PemCertsConfigTest {

    private static final String TEMP_CERT_PREFIX = "PemCertsConfigTest";

    private record PemCertsConfigImpl(Optional<List<Path>> certs, Optional<List<Path>> certDirs) implements PemCertsConfig {
    }

    @Test
    void testHasNoTrustedCertificates() throws IOException {
        Path emptyTempCertDir = Files.createTempDirectory(TEMP_CERT_PREFIX);
        Path tempCertDirWith1File = Files.createTempDirectory(TEMP_CERT_PREFIX);
        Path certInTempCertDirWith1File = tempCertDirWith1File.resolve("ts.pem");
        Files.createFile(certInTempCertDirWith1File);
        Path tempCertDirWith2Files = Files.createTempDirectory(TEMP_CERT_PREFIX);
        Files.createFile(tempCertDirWith2Files.resolve("ts-1.pem"));
        Files.createFile(tempCertDirWith2Files.resolve("ts-2.pem"));

        var noCertsFoundConfig = new PemCertsConfigImpl(Optional.of(List.of()), Optional.of(List.of(emptyTempCertDir)));
        assertThat(noCertsFoundConfig.hasNoTrustedCertificates()).isTrue();

        var onlyCertFileConfig = new PemCertsConfigImpl(Optional.of(List.of(certInTempCertDirWith1File)), Optional.empty());
        assertThat(onlyCertFileConfig.hasNoTrustedCertificates()).isFalse();

        var certFileAndEmptyDirConfig = new PemCertsConfigImpl(Optional.of(List.of(certInTempCertDirWith1File)),
                Optional.of(List.of(emptyTempCertDir)));
        assertThat(certFileAndEmptyDirConfig.hasNoTrustedCertificates()).isFalse();

        var certFileAndCertDirConfig = new PemCertsConfigImpl(Optional.of(List.of(certInTempCertDirWith1File)),
                Optional.of(List.of(tempCertDirWith2Files)));
        assertThat(certFileAndCertDirConfig.hasNoTrustedCertificates()).isFalse();

        var certDirConfig = new PemCertsConfigImpl(Optional.empty(), Optional.of(List.of(tempCertDirWith2Files)));
        assertThat(certDirConfig.hasNoTrustedCertificates()).isFalse();

        var multipleCertDirsConfig = new PemCertsConfigImpl(Optional.empty(),
                Optional.of(List.of(tempCertDirWith1File, tempCertDirWith2Files)));
        assertThat(multipleCertDirsConfig.hasNoTrustedCertificates()).isFalse();
    }

    @Test
    void testToOptions() throws IOException {
        Path emptyTempCertDir = Files.createTempDirectory(TEMP_CERT_PREFIX);
        Path firstCaCert = Path.of("target/certs/pem-certs-config-test-1-ca.crt");
        Path secondCaCert = Path.of("target/certs/pem-certs-config-test-2-ca.crt");
        Path tempDirWith1File1st = Files.createTempDirectory(TEMP_CERT_PREFIX);
        Files.copy(firstCaCert, tempDirWith1File1st.resolve("ca.crt"));
        Path tempDirWith1File2nd = Files.createTempDirectory(TEMP_CERT_PREFIX);
        Files.copy(secondCaCert, tempDirWith1File2nd.resolve("ca.crt"));
        Path tempDirWith2Files = Files.createTempDirectory(TEMP_CERT_PREFIX);
        Files.copy(firstCaCert, tempDirWith2Files.resolve("ca-1.crt"));
        Files.copy(secondCaCert, tempDirWith2Files.resolve("ca-2.crt"));

        var noCertsFoundConfig = new PemCertsConfigImpl(Optional.of(List.of()), Optional.of(List.of(emptyTempCertDir)));
        assertThatThrownBy(noCertsFoundConfig::toOptions).isInstanceOf(IllegalArgumentException.class);

        var oneCertDirWithOneFileConfig = new PemCertsConfigImpl(Optional.empty(), Optional.of(List.of(tempDirWith1File1st)));
        assertThat(oneCertDirWithOneFileConfig.toOptions().getCertValues()).hasSize(1);

        var oneCertAndOneCertDirWithFileConfig = new PemCertsConfigImpl(Optional.of(List.of(firstCaCert)),
                Optional.of(List.of(tempDirWith1File2nd)));
        assertThat(oneCertAndOneCertDirWithFileConfig.toOptions().getCertValues()).hasSize(2);

        var twoCertDirsConfig = new PemCertsConfigImpl(Optional.empty(),
                Optional.of(List.of(tempDirWith1File1st, tempDirWith1File2nd)));
        assertThat(twoCertDirsConfig.toOptions().getCertValues()).hasSize(2);

        var oneCertDirWith2Files = new PemCertsConfigImpl(Optional.empty(), Optional.of(List.of(tempDirWith2Files)));
        assertThat(oneCertDirWith2Files.toOptions().getCertValues()).hasSize(2);
    }

}
