package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TrustStoreUtils;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.jwt.util.ResourceUtils;
import io.vertx.core.json.JsonObject;

public class TestUtils {

    public static String createTokenWithInlinedCertChain(String preferredUserName) throws Exception {
        X509Certificate rootCert = KeyUtils.getCertificate(ResourceUtils.readResource("/ca.cert.pem"));
        X509Certificate intermediateCert = KeyUtils.getCertificate(ResourceUtils.readResource("/intermediate.cert.pem"));
        X509Certificate subjectCert = KeyUtils.getCertificate(ResourceUtils.readResource("/www.quarkustest.com.cert.pem"));
        PrivateKey subjectPrivateKey = KeyUtils.readPrivateKey("/www.quarkustest.com.key.pem");

        String bearerAccessToken = getAccessTokenWithCertChain(
                List.of(subjectCert, intermediateCert, rootCert),
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
