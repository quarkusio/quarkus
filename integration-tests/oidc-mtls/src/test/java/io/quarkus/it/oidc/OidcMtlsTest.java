package io.quarkus.it.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.client.KeycloakTestClient.Tls;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class OidcMtlsTest {

    KeycloakTestClient client = new KeycloakTestClient(
            new Tls("target/certificates/oidc-client-keystore.p12",
                    "target/certificates/oidc-client-truststore.p12"));

    @TestHTTPResource(tls = true)
    URL url;

    private static Vertx vertx;

    @BeforeAll
    public static void createVertx() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    public static void closeVertx() {
        if (vertx != null) {
            vertx.close();
        }
    }

    @Test
    public void testMtlsJwt() throws Exception {
        WebClientOptions options = createWebClientOptions();
        WebClient webClient = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);

        // HTTP 200
        HttpResponse<io.vertx.mutiny.core.buffer.Buffer> resp = webClient.get("/service/mtls-jwt")
                .putHeader("Authorization",
                        OidcConstants.BEARER_SCHEME + " " + getAccessToken("backend-service", null, "alice"))
                .send().await()
                .indefinitely();
        assertEquals(200, resp.statusCode());
        String name = resp.bodyAsString();
        assertEquals("Identities: CN=backend-service, alice;"
                + " Client: backend-service;"
                + " JWT cert thumbprint: true, introspection cert thumbprint: false", name);

        // HTTP 401, invalid token
        resp = webClient.get("/service/mtls-jwt")
                .putHeader("Authorization", OidcConstants.BEARER_SCHEME + " " + "123")
                .send().await()
                .indefinitely();
        assertEquals(401, resp.statusCode());

        // HTTP 401, no token and inclusive authentication in the strict mode
        resp = webClient.get("/service/mtls-jwt").send().await().indefinitely();
        assertEquals(401, resp.statusCode());
    }

    @Test
    public void testMtlsIntrospection() throws Exception {
        WebClientOptions options = createWebClientOptions();
        WebClient webClient = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);

        // HTTP 200
        HttpResponse<io.vertx.mutiny.core.buffer.Buffer> resp = webClient.get("/service/mtls-introspection")
                .putHeader("Authorization",
                        OidcConstants.BEARER_SCHEME + " " + getAccessToken("backend-service", null, "alice"))
                .send().await()
                .indefinitely();
        assertEquals(200, resp.statusCode());
        String name = resp.bodyAsString();
        assertEquals("Identities: CN=backend-service, alice;"
                + " Client: backend-service;"
                + " JWT cert thumbprint: false, introspection cert thumbprint: true", name);

        // HTTP 401, invalid token
        resp = webClient.get("/service/mtls-introspection")
                .putHeader("Authorization", OidcConstants.BEARER_SCHEME + " " + "123")
                .send().await()
                .indefinitely();
        assertEquals(401, resp.statusCode());
    }

    @Test
    public void testMtlsClientWithSecret() throws Exception {
        WebClientOptions options = createWebClientOptions();
        WebClient webClient = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);

        String accessToken = getAccessToken("backend-client-with-secret", "secret", "alice");
        // HTTP 200
        HttpResponse<io.vertx.mutiny.core.buffer.Buffer> resp = webClient.get("/service/mtls-client-with-secret")
                .putHeader("Authorization",
                        OidcConstants.BEARER_SCHEME + " " + accessToken)
                .send().await()
                .indefinitely();
        assertEquals(200, resp.statusCode());
        String name = resp.bodyAsString();
        assertEquals("Identities: CN=backend-service, alice;"
                + " Client: backend-client-with-secret;"
                + " JWT cert thumbprint: false, introspection cert thumbprint: false", name);

        // HTTP 401, token is valid but it is not certificate bound
        resp = webClient.get("/service/mtls-jwt")
                .putHeader("Authorization", OidcConstants.BEARER_SCHEME + " " + accessToken)
                .send().await()
                .indefinitely();
        assertEquals(401, resp.statusCode());
    }

    private String getAccessToken(String clientName, String clientSecret, String userName) {
        return client.getAccessToken(userName, userName, clientName, clientSecret);
    }

    private WebClientOptions createWebClientOptions() throws Exception {
        return createWebClientOptions(url);
    }

    static WebClientOptions createWebClientOptions(URL url) throws Exception {
        WebClientOptions webClientOptions = new WebClientOptions().setDefaultHost(url.getHost())
                .setDefaultPort(url.getPort()).setSsl(true).setVerifyHost(false);

        byte[] keyStoreData = getFileContent(Paths.get("target/certificates/oidc-client-keystore.p12"));
        KeyStoreOptions keyStoreOptions = new KeyStoreOptions()
                .setPassword("password")
                .setValue(Buffer.buffer(keyStoreData))
                .setType("PKCS12");
        webClientOptions.setKeyCertOptions(keyStoreOptions);

        byte[] trustStoreData = getFileContent(Paths.get("target/certificates/oidc-client-truststore.p12"));
        KeyStoreOptions trustStoreOptions = new KeyStoreOptions()
                .setPassword("password")
                .setValue(Buffer.buffer(trustStoreData))
                .setType("PKCS12");
        webClientOptions.setTrustOptions(trustStoreOptions);

        return webClientOptions;
    }

    private static byte[] getFileContent(Path path) throws IOException {
        byte[] data;
        final InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(ClassPathUtils.toResourceName(path));
        if (resource != null) {
            try (InputStream is = resource) {
                data = doRead(is);
            }
        } else {
            try (InputStream is = Files.newInputStream(path)) {
                data = doRead(is);
            }
        }
        return data;
    }

    private static byte[] doRead(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

}
