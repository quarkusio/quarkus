package io.quarkus.it.bouncycastle;

import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.awaitility.core.ThrowingRunnable;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@QuarkusTest
public class BouncyCastleFipsJsseTestCase {

    static final Logger LOG = Logger.getLogger(BouncyCastleFipsJsseTestCase.class);

    @TestHTTPResource(ssl = true)
    URL url;

    @Inject
    Vertx vertx;

    @Test
    public void testListProviders() throws Exception {
        doTestListProviders();
        checkLog(false);
    }

    protected void doTestListProviders() throws Exception {
        WebClientOptions options = createWebClientOptions();
        WebClient webClient = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);
        HttpResponse<io.vertx.mutiny.core.buffer.Buffer> resp = webClient.get("/jsse/listProviders").send().await()
                .indefinitely();
        String providers = resp.bodyAsString();
        assertTrue(providers.contains("BCFIPS,BCJSSE"));
    }

    private WebClientOptions createWebClientOptions() throws Exception {
        WebClientOptions webClientOptions = new WebClientOptions().setDefaultHost(url.getHost())
                .setDefaultPort(url.getPort()).setSsl(true).setVerifyHost(false);

        byte[] keyStoreData = getFileContent(Paths.get("client-keystore.jks"));
        KeyStoreOptions keyStoreOptions = new KeyStoreOptions()
                .setPassword("password")
                .setValue(Buffer.buffer(keyStoreData))
                .setType("BCFKS")
                .setProvider("BCFIPS");
        webClientOptions.setKeyCertOptions(keyStoreOptions);

        byte[] trustStoreData = getFileContent(Paths.get("client-truststore.jks"));
        KeyStoreOptions trustStoreOptions = new KeyStoreOptions()
                .setPassword("password")
                .setValue(Buffer.buffer(trustStoreData))
                .setType("BCFKS")
                .setProvider("BCFIPS");
        webClientOptions.setTrustOptions(trustStoreOptions);

        return webClientOptions;
    }

    protected void checkLog(boolean serverOnly) {
        final Path logDirectory = Paths.get(".", "target");
        given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        Path accessLogFilePath = logDirectory.resolve("quarkus.log");
                        boolean fileExists = Files.exists(accessLogFilePath);
                        if (!fileExists) {
                            accessLogFilePath = logDirectory.resolve("target/quarkus.log");
                            fileExists = Files.exists(accessLogFilePath);
                        }
                        Assertions.assertTrue(fileExists, "access log file " + accessLogFilePath + " is missing");

                        boolean checkClientPassed = serverOnly;
                        boolean checkServerPassed = false;

                        StringBuilder sbLog = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(accessLogFilePath)),
                                        StandardCharsets.UTF_8))) {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                sbLog.append(line).append("/r/n");
                                if (!checkServerPassed && line.contains("ProvTlsServer")
                                        && (line.contains("Server selected protocol version: TLSv1.2")
                                                || line.contains("Server selected protocol version: TLSv1.3"))) {
                                    checkServerPassed = true;
                                } else if (!checkClientPassed && line.contains("ProvTlsClient")
                                        && (line.contains("Client notified of selected protocol version: TLSv1.2")
                                                || line.contains("Client notified of selected protocol version: TLSv1.3"))) {
                                    checkClientPassed = true;
                                }
                                if (checkClientPassed && checkServerPassed) {
                                    break;
                                }
                            }
                        }
                        assertTrue(checkClientPassed,
                                "Log file doesn't contain BouncyCastle JSSE client records, log: " + sbLog.toString());
                        assertTrue(checkServerPassed,
                                "Log file doesn't contain BouncyCastle JSSE server records, log: " + sbLog.toString());
                    }
                });
    }

    private static byte[] getFileContent(Path path) throws IOException {
        byte[] data;
        final InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.toString());
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
