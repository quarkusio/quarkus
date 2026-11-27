package io.quarkus.security.webauthn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * Checks the root certificates trusted by default to verify WebAuthn attestations.
 * <p>
 * Nothing can be signed with these roots, so we check that we trust the right certificates, and that real
 * attestation certificate chains of Android devices lead to them.
 */
class DefaultAttestationTrustAnchorsTest {

    // SHA-256 fingerprints, from the certificates published by their issuers.

    /**
     * Android Keystore Software Attestation Root, from
     * https://github.com/android/keyattestation/blob/main/src/main/kotlin/SoftwareRoot.kt
     */
    private static final String ANDROID_SOFTWARE_ROOT = "51D496AD4664190FBDAF1AD987278EFA35D6FBF52E50A9C34DED8395477EDE5C";
    /**
     * Google Hardware Attestation Root (RSA), from https://android.googleapis.com/attestation/root
     */
    private static final String ANDROID_HARDWARE_ROOT_RSA = "CEDB1CB6DC896AE5EC797348BCE9286753C2B38EE71CE0FBE34A9A1248800DFC";
    /**
     * Google Key Attestation CA1 (ECDSA), from https://android.googleapis.com/attestation/root
     */
    private static final String ANDROID_HARDWARE_ROOT_ECDSA = "6D9DB4CE6C5C0B293166D08986E05774A8776CEB525D9E4329520DE12BA4BCC0";
    /**
     * Apple WebAuthn Root CA, from https://www.apple.com/certificateauthority/Apple_WebAuthn_Root_CA.pem
     */
    private static final String APPLE_WEBAUTHN_ROOT = "0915DD5C07A28DB549D1F677BB5A75D4BFBE9561A773424327762E9E02F9BB29";
    /**
     * GlobalSign Root CA - R3, from https://secure.globalsign.com/cacert/root-r3.crt
     */
    private static final String GLOBALSIGN_ROOT_R3 = "CBB522D7B7F127AD6A0113865BDF1CD4102E7D0759AF635A7CF4720DC963C53B";
    /**
     * GlobalSign Root R46, from https://secure.globalsign.com/cacert/rootr46.crt
     */
    private static final String GLOBALSIGN_ROOT_R46 = "4FA3126D8D3A11D1C4855A4F807CBAD6CF919D3A5A88B03BEA2C6372D93C40C9";

    @Test
    void defaultTrustAnchors() throws Exception {
        assertThat(fingerprints(WebAuthnSecurity.defaultAttestationTrustStore()))
                .containsExactlyInAnyOrder(ANDROID_SOFTWARE_ROOT, ANDROID_HARDWARE_ROOT_RSA, ANDROID_HARDWARE_ROOT_ECDSA,
                        APPLE_WEBAUTHN_ROOT, GLOBALSIGN_ROOT_R3, GLOBALSIGN_ROOT_R46);
    }

    /**
     * Real chain, from https://github.com/android/keyattestation (Apache License 2.0), of a key attested by a device
     * with an attestation key provisioned in the factory, chaining to the RSA root.
     */
    @Test
    void androidHardwareRootRsa() throws Exception {
        assertThat(validate("blueline-rsa-root.pem")).isEqualTo(ANDROID_HARDWARE_ROOT_RSA);
    }

    /**
     * Real chain, from the same source, of a key attested by a device with a remotely provisioned attestation key,
     * chaining to the ECDSA root.
     */
    @Test
    void androidHardwareRootEcdsa() throws Exception {
        assertThat(validate("tegu-ecdsa-root.pem")).isEqualTo(ANDROID_HARDWARE_ROOT_ECDSA);
    }

    /**
     * Real chain, from the same source, of a key attested by software, chaining to the Android Keystore Software Attestation
     * Root.
     */
    @Test
    void androidSoftwareRoot() throws Exception {
        assertThat(validate("marlin-software-root.pem")).isEqualTo(ANDROID_SOFTWARE_ROOT);
    }

    @Test
    void chainWithoutTrustedRoot() throws Exception {
        // only the leaf and the first intermediate, so that the chain does not reach any root
        List<X509Certificate> chain = chain("tegu-ecdsa-root.pem");
        List<X509Certificate> truncated = chain.subList(0, 2);
        assertThatThrownBy(() -> validate(truncated, WebAuthnSecurity.defaultAttestationTrustStore()))
                .isInstanceOf(CertPathValidatorException.class)
                .hasMessageContaining("Path does not chain with any of the trust anchors");
    }

    /**
     * Validates the chain against the default trust anchors, as of the date at which all its certificates were valid,
     * because these chains come from tests of real devices, and some of their certificates have expired since.
     *
     * @return the SHA-256 fingerprint of the trust anchor the chain leads to
     */
    private static String validate(String resource) throws Exception {
        return validate(chain(resource), WebAuthnSecurity.defaultAttestationTrustStore());
    }

    private static String validate(List<X509Certificate> chain, KeyStore trustStore) throws Exception {
        Date date = new Date(0);
        for (X509Certificate certificate : chain) {
            if (certificate.getNotBefore().after(date)) {
                date = certificate.getNotBefore();
            }
        }
        // the chain contains its root, which is not part of the path to validate but is the trust anchor
        List<X509Certificate> path = new ArrayList<>(chain);
        X509Certificate last = path.get(path.size() - 1);
        if (last.getSubjectX500Principal().equals(last.getIssuerX500Principal())) {
            path.remove(path.size() - 1);
        }
        PKIXParameters parameters = new PKIXParameters(trustStore);
        parameters.setRevocationEnabled(false);
        parameters.setDate(new Date(date.getTime() + 60_000));
        CertPath certPath = CertificateFactory.getInstance("X.509").generateCertPath(path);
        var result = (java.security.cert.PKIXCertPathValidatorResult) CertPathValidator.getInstance("PKIX")
                .validate(certPath, parameters);
        return fingerprint(result.getTrustAnchor().getTrustedCert());
    }

    private static List<X509Certificate> chain(String resource) throws Exception {
        try (InputStream in = DefaultAttestationTrustAnchorsTest.class
                .getResourceAsStream("/attestation/android/" + resource)) {
            assertThat(in).as(resource).isNotNull();
            List<X509Certificate> chain = new ArrayList<>();
            for (var certificate : CertificateFactory.getInstance("X.509").generateCertificates(in)) {
                chain.add((X509Certificate) certificate);
            }
            return chain;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Set<String> fingerprints(KeyStore trustStore) throws Exception {
        Set<String> fingerprints = new TreeSet<>();
        for (String alias : Collections.list(trustStore.aliases())) {
            fingerprints.add(fingerprint((X509Certificate) trustStore.getCertificate(alias)));
        }
        return fingerprints;
    }

    private static String fingerprint(X509Certificate certificate) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
    }
}
