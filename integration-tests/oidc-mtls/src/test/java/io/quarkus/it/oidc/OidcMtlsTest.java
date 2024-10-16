package io.quarkus.it.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@QuarkusTest
public class OidcMtlsTest {

    @TestHTTPResource(ssl = true)
    URL url;

    KeycloakTestClient keycloakClient = new KeycloakTestClient();

    @Test
    public void testGetIdentityNames() throws Exception {
        Vertx vertx = Vertx.vertx();
        try {
            WebClientOptions options = createWebClientOptions();
            WebClient webClient = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);

            // HTTP 200
            HttpResponse<io.vertx.mutiny.core.buffer.Buffer> resp = webClient.get("/service/name")
                    .putHeader("Authorization", OidcConstants.BEARER_SCHEME + " " + keycloakClient.getAccessToken("alice"))
                    .send().await()
                    .indefinitely();
            assertEquals(200, resp.statusCode());
            String name = resp.bodyAsString();
            assertEquals("Identities: CN=client, alice", name);

            // HTTP 401, invalid token
            resp = webClient.get("/service/name")
                    .putHeader("Authorization", OidcConstants.BEARER_SCHEME + " " + "123")
                    .send().await()
                    .indefinitely();
            assertEquals(401, resp.statusCode());
        } finally {
            vertx.close();
        }
    }

    private WebClientOptions createWebClientOptions() throws Exception {
        WebClientOptions webClientOptions = new WebClientOptions().setDefaultHost(url.getHost())
                .setDefaultPort(url.getPort()).setSsl(true).setVerifyHost(false);

        byte[] keyStoreData = getFileContent(Paths.get("client-keystore.jks"));
        KeyStoreOptions keyStoreOptions = new KeyStoreOptions()
                .setPassword("password")
                .setValue(Buffer.buffer(keyStoreData))
                .setType("JKS");
        webClientOptions.setKeyCertOptions(keyStoreOptions);

        byte[] trustStoreData = getFileContent(Paths.get("client-truststore.jks"));
        KeyStoreOptions trustStoreOptions = new KeyStoreOptions()
                .setPassword("secret")
                .setValue(Buffer.buffer(trustStoreData))
                .setType("JKS");
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
