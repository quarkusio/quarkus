package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TrustStoreUtils;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;
import io.vertx.core.json.JsonObject;

public class TestUtils {

    public static List<X509Certificate> loadCertificateChain() throws Exception {
        Path rootCertPath = Paths.get("target/chain/root.crt");
        Path intermediateCertPath = Paths.get("target/chain/intermediate.crt");
        Path leafCertPath = Paths.get("target/chain/www.quarkustest.com.crt");

        X509Certificate rootCert = KeyUtils.getCertificate(Files.readString(rootCertPath));
        X509Certificate intermediateCert = KeyUtils.getCertificate(Files.readString(intermediateCertPath));
        X509Certificate subjectCert = KeyUtils.getCertificate(Files.readString(leafCertPath));

        return List.of(subjectCert, intermediateCert, rootCert);
    }

    public static PrivateKey loadLeafCertificatePrivateKey() throws Exception {
        Path leafKeyPath = Paths.get("target/chain/www.quarkustest.com.key");
        return KeyUtils.decodePrivateKey(Files.readString(leafKeyPath));
    }

    public static String createTokenWithInlinedCertChain(String preferredUserName) throws Exception {
        List<X509Certificate> chain = loadCertificateChain();
        PrivateKey subjectPrivateKey = loadLeafCertificatePrivateKey();

        String bearerAccessToken = getAccessTokenWithCertChain(
                chain,
                subjectPrivateKey,
                preferredUserName);

        assertX5cOnlyIsPresent(bearerAccessToken);
        return bearerAccessToken;
    }

    public static String getAccessTokenWithCertChain(List<X509Certificate> chain,
            PrivateKey privateKey, String preferredUserName) throws Exception {
        return Jwt.preferredUserName(preferredUserName)
                .groups("admin")
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .claim("root-certificate-thumbprint", TrustStoreUtils.calculateThumprint(chain.get(chain.size() - 1)))
                .jws().chain(chain)
                .sign(privateKey);
    }

    public static void assertX5cOnlyIsPresent(String token) {
        JsonObject headers = OidcUtils.decodeJwtHeaders(token);
        assertTrue(headers.containsKey("x5c"));
        assertFalse(headers.containsKey("kid"));
        assertFalse(headers.containsKey("x5t"));
        assertFalse(headers.containsKey("x5t#S256"));
    }

}
