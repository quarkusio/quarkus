package io.quarkus.consul.config.runtime;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyStoreOptionsBase;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class VertxConsulConfigGateway implements ConsulConfigGateway {

    private static final Logger log = Logger.getLogger(VertxConsulConfigGateway.class);

    private static final String PKS_12 = "PKS12";
    private static final String JKS = "JKS";

    private final ConsulConfig consulConfig;
    private final Vertx vertx;
    private final WebClient webClient;

    public VertxConsulConfigGateway(ConsulConfig consulConfig) {
        this.consulConfig = consulConfig;
        this.vertx = Vertx.vertx();
        this.webClient = createHttpClient(vertx, consulConfig.agent);
    }

    public static WebClient createHttpClient(Vertx vertx, ConsulConfig.AgentConfig agentConfig) {

        WebClientOptions webClientOptions = new WebClientOptions()
                .setConnectTimeout((int) agentConfig.connectionTimeout.toMillis())
                .setIdleTimeout((int) agentConfig.readTimeout.getSeconds());

        boolean trustAll = agentConfig.trustCerts;
        try {
            if (agentConfig.trustStore.isPresent()) {
                Path trustStorePath = agentConfig.trustStore.get();
                String type = determineStoreType(trustStorePath);
                KeyStoreOptionsBase storeOptions = storeOptions(trustStorePath, agentConfig.trustStorePassword,
                        createStoreOptions(type));
                if (isPfx(type)) {
                    webClientOptions.setPfxTrustOptions((PfxOptions) storeOptions);
                } else {
                    webClientOptions.setTrustStoreOptions((JksOptions) storeOptions);
                }
            } else if (trustAll) {
                skipVerify(webClientOptions);
            } else if (agentConfig.keyStore.isPresent()) {
                Path trustStorePath = agentConfig.keyStore.get();
                String type = determineStoreType(trustStorePath);
                KeyStoreOptionsBase storeOptions = storeOptions(trustStorePath, agentConfig.keyStorePassword,
                        createStoreOptions(type));
                if (isPfx(type)) {
                    webClientOptions.setPfxTrustOptions((PfxOptions) storeOptions);
                } else {
                    webClientOptions.setTrustStoreOptions((JksOptions) storeOptions);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return WebClient.create(vertx, webClientOptions);
    }

    private static void skipVerify(WebClientOptions options) {
        options.setTrustAll(true);
        options.setVerifyHost(false);
    }

    private static KeyStoreOptionsBase createStoreOptions(String type) {
        if (isPfx(type)) {
            return new PfxOptions();
        }
        return new JksOptions();
    }

    private static boolean isPfx(String type) {
        return PKS_12.equals(type);
    }

    private static <T extends KeyStoreOptionsBase> KeyStoreOptionsBase storeOptions(Path storePath,
            Optional<String> storePassword, T store) throws Exception {
        return store
                .setPassword(storePassword.orElse(""))
                .setValue(io.vertx.core.buffer.Buffer.buffer(storeBytes(storePath)));
    }

    private static String determineStoreType(Path keyStorePath) {
        String pathName = keyStorePath.toString().toLowerCase();
        if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
            return PKS_12;
        }
        return JKS;
    }

    private static byte[] storeBytes(Path keyStorePath)
            throws Exception {
        InputStream classPathResource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(keyStorePath.toString());
        if (classPathResource != null) {
            try (InputStream is = classPathResource) {
                return allBytes(is);
            }
        } else {
            try (InputStream is = Files.newInputStream(keyStorePath)) {
                return allBytes(is);
            }
        }
    }

    private static byte[] allBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    @Override
    public Uni<Response> getValue(String key) {
        HttpRequest<Buffer> request = webClient
                .get(consulConfig.agent.hostPort.getPort(), consulConfig.agent.hostPort.getHostString(), "/v1/kv/" + key)
                .ssl(consulConfig.agent.useHttps)
                .putHeader("Accept", "application/json;charset=UTF-8");
        if (consulConfig.agent.token.isPresent()) {
            request.putHeader("Authorization", "Bearer " + consulConfig.agent.token.get());
        }

        log.debug("Attempting to look up value of key '" + key + "' from Consul.");
        return request.send().map(r -> {
            if (r.statusCode() != 200) {
                log.debug("Look up of key '" + key + "' from Consul yielded a non success HTTP error-code: " + r.statusCode());
                return null;
            } else {
                JsonArray jsonArray = r.bodyAsJsonArray();
                if (jsonArray.size() != 1) {
                    throw new IllegalStateException(
                            "Consul returned an unexpected number of results when looking up value of key '" + key + "'");
                }
                JsonObject jsonObject = jsonArray.getJsonObject(0);
                return new Response(jsonObject.getString("Key"), jsonObject.getString("Value"));
            }
        });
    }

    @Override
    public void close() {
        this.webClient.close();
        this.vertx.closeAndAwait();
    }
}
