package io.quarkus.spiffe.client.deployment.test;

import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.mutiny.core.Vertx.newInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.spiffe.client.SpiffeAuthorizationException;
import io.quarkus.spiffe.client.SpiffeClient;
import io.quarkus.spiffe.client.SpiffeConnectionException;
import io.quarkus.spiffe.client.WorkloadCertificateDocument;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;

@QuarkusTestResource(SpiffeX509TestResource.class)
class SpiffeX509SvidTest {

    private static final String DEFAULT_SPIFFE_ID = "spiffe://test.quarkus.io/test-workload";
    private static final String MODE_PATH = "/api/mode";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .overrideRuntimeConfigKey("quarkus.spiffe-client.endpoint-socket", "${x509.quarkus.spiffe-client.endpoint-socket}");

    @Inject
    Vertx vertx;

    @Inject
    SpiffeClient spiffeClient;

    @AfterEach
    void resetMode() {
        setMode("HEALTHY");
    }

    @Test
    void getWorkloadCertificateSubject() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.subject()).isEqualTo(DEFAULT_SPIFFE_ID);
        assertThat(cert.certificateChain().certificateChainPem()).isNotEmpty();
        assertThat(cert.certificateChain().certificateChainPem().size())
                .isEqualTo(cert.certificateChain().certificateChain().size());
        for (int i = 0; i < cert.certificateChain().certificateChain().size(); i++) {
            assertThat(cert.certificateChain().certificateChain().get(i).getEncoded())
                    .isEqualTo(parsePemDer(cert.certificateChain().certificateChainPem().get(i), "CERTIFICATE"));
        }
    }

    @Test
    void getWorkloadCertificateLeafHasSpiffeIdInSanUri() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.certificateChain().certificateChain()).isNotEmpty();
        var leaf = cert.certificateChain().certificateChain().get(0);
        var sans = leaf.getSubjectAlternativeNames();
        assertThat(sans).isNotNull();
        // URI SAN type = 6
        String spiffeUri = sans.stream()
                .filter(san -> (int) san.get(0) == 6)
                .map(san -> san.get(1).toString())
                .findFirst()
                .orElse(null);
        assertThat(spiffeUri).isEqualTo(DEFAULT_SPIFFE_ID);
    }

    @Test
    void getWorkloadCertificatePrivateKeyMatchesCert() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        verifyKeyPairMatches(cert);
        assertThat(cert.certificateChain().privateKeyPem()).startsWith("-----BEGIN PRIVATE KEY-----");
        assertThat(cert.certificateChain().privateKey().getEncoded())
                .isEqualTo(parsePemDer(cert.certificateChain().privateKeyPem(), "PRIVATE KEY"));
    }

    @Test
    void getWorkloadCertificateTrustBundleValidatesChain() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.trustBundle().certificateChain()).isNotEmpty();
        validateCertificateChain(cert.certificateChain().certificateChain(), cert.trustBundle().certificateChain());
        assertThat(cert.trustBundle().certificateChainPem()).isNotEmpty();
        assertThat(cert.trustBundle().certificateChainPem().size()).isEqualTo(cert.trustBundle().certificateChain().size());
        for (int i = 0; i < cert.trustBundle().certificateChain().size(); i++) {
            assertThat(cert.trustBundle().certificateChain().get(i).getEncoded())
                    .isEqualTo(parsePemDer(cert.trustBundle().certificateChainPem().get(i), "CERTIFICATE"));
        }
    }

    @Test
    void getWorkloadCertificatePermissionDenied() {
        setMode("PERMISSION_DENIED");
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeAuthorizationException.class);
    }

    @Test
    void getWorkloadCertificateServerUnavailable() {
        setMode("UNAVAILABLE");
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeConnectionException.class);
    }

    @Test
    void getWorkloadCertificateRecoveryAfterGrpcDown() {
        WorkloadCertificateDocument cert = fetchCert();
        assertThat(cert.subject()).isEqualTo(DEFAULT_SPIFFE_ID);

        setMode("GRPC_DOWN");
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeConnectionException.class);

        setMode("HEALTHY");
        cert = fetchCert();
        assertThat(cert.subject()).isEqualTo(DEFAULT_SPIFFE_ID);
    }

    // proto spiffe_id and leaf cert URI SAN must agree on the identity
    @Test
    void getWorkloadCertificateProtoSpiffeIdMatchesSanUri() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        var uriSans = extractUriSans(cert);
        assertThat(uriSans).hasSize(1);
        assertThat(cert.subject()).isEqualTo(uriSans.get(0));
    }

    @Test
    void getWorkloadCertificateLeafIsCurrentlyValid() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        cert.certificateChain().certificateChain().get(0).checkValidity();
    }

    @Test
    void getWorkloadCertificateTrustBundleValidatesChainDepth2() throws Exception {
        setMode("CHAIN_DEPTH_2");
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.certificateChain().certificateChain()).hasSize(2);
        validateCertificateChain(cert.certificateChain().certificateChain(), cert.trustBundle().certificateChain());
    }

    @Test
    void getWorkloadCertificateEcP384() throws Exception {
        setMode("EC_P384");
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.certificateChain().privateKey().getAlgorithm()).isEqualTo("EC");
        verifyKeyPairMatches(cert);
    }

    @Test
    void getWorkloadCertificateRsa2048() throws Exception {
        setMode("RSA_2048");
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.certificateChain().privateKey().getAlgorithm()).isEqualTo("RSA");
        verifyKeyPairMatches(cert);
    }

    // X.509-SVID 4.1: leaf SVID must have cA=false
    @Test
    void getWorkloadCertificateLeafIsNotCa() {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.certificateChain().certificateChain().get(0).getBasicConstraints()).isEqualTo(-1);
    }

    // X.509-SVID 4.3: leaf SVID must set digitalSignature
    @Test
    void getWorkloadCertificateLeafHasDigitalSignatureKeyUsage() {
        WorkloadCertificateDocument cert = fetchCert();

        boolean[] keyUsage = cert.certificateChain().certificateChain().get(0).getKeyUsage();
        assertThat(keyUsage).isNotNull();
        assertThat(keyUsage[0]).isTrue();
    }

    // X.509-SVID 4.3: leaf SVID must not have keyCertSign — a leaf that can sign certs could forge identities
    @Test
    void getWorkloadCertificateLeafDoesNotHaveKeyCertSign() {
        WorkloadCertificateDocument cert = fetchCert();

        boolean[] keyUsage = cert.certificateChain().certificateChain().get(0).getKeyUsage();
        assertThat(keyUsage).isNotNull();
        assertThat(keyUsage[5]).isFalse();
    }

    // X.509-SVID 2: leaf must have exactly one URI SAN containing the SPIFFE ID
    @Test
    void getWorkloadCertificateLeafHasExactlyOneUriSan() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(extractUriSans(cert)).hasSize(1);
    }

    @Test
    void getWorkloadCertificateReturnsDefaultIdentity() {
        setMode("MULTI_SVID");
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.subject()).isEqualTo(DEFAULT_SPIFFE_ID);
    }

    @Test
    void getWorkloadCertificateEmptySvids() {
        setMode("EMPTY_SVIDS");
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeConnectionException.class);
    }

    @Test
    void getWorkloadCertificateCorruptedCert() {
        setMode("CORRUPTED_CERT");
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeConnectionException.class);
    }

    // --- helpers ---

    private static void validateCertificateChain(List<X509Certificate> chain, List<X509Certificate> trustBundle)
            throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        CertPath path = cf.generateCertPath(chain);
        Set<TrustAnchor> anchors = trustBundle.stream()
                .map(ca -> new TrustAnchor(ca, null))
                .collect(Collectors.toSet());
        PKIXParameters params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false);
        CertPathValidator.getInstance("PKIX").validate(path, params);
    }

    private static List<String> extractUriSans(WorkloadCertificateDocument cert) throws Exception {
        var sans = cert.certificateChain().certificateChain().get(0).getSubjectAlternativeNames();
        if (sans == null) {
            return List.of();
        }
        return sans.stream()
                .filter(san -> (int) san.get(0) == 6)
                .map(san -> san.get(1).toString())
                .toList();
    }

    private static void verifyKeyPairMatches(WorkloadCertificateDocument cert) throws Exception {
        byte[] data = "spiffe-key-pair-test".getBytes();
        var leaf = cert.certificateChain().certificateChain().get(0);
        Signature sig = Signature.getInstance(leaf.getSigAlgName());
        sig.initSign(cert.certificateChain().privateKey());
        sig.update(data);
        byte[] signed = sig.sign();
        sig.initVerify(leaf.getPublicKey());
        sig.update(data);
        assertThat(sig.verify(signed)).isTrue();
    }

    private static byte[] parsePemDer(String pem, String type) {
        String base64 = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private WorkloadCertificateDocument fetchCert() {
        return spiffeClient.getWorkloadCertificate().await().atMost(TIMEOUT);
    }

    private void setMode(String mode) {
        adminRequest(POST, MODE_PATH, Buffer.buffer(mode));
    }

    private String adminRequest(HttpMethod method, String path, Buffer body) {
        URI uri = URI.create(ConfigProvider.getConfig().getValue("spiffe.x509.test.admin-url", String.class));
        var client = newInstance(vertx).createHttpClient();
        try {
            return client.request(method, uri.getPort(), uri.getHost(), path)
                    .flatMap(req -> body != null ? req.send(body) : req.send())
                    .flatMap(resp -> {
                        assertThat(resp.statusCode()).isEqualTo(200);
                        return resp.body();
                    })
                    .await().atMost(TIMEOUT)
                    .toString();
        } finally {
            client.close().await().atMost(TIMEOUT);
        }
    }
}
