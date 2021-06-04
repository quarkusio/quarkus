package io.quarkus.spring.cloud.config.client.runtime;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.spring.cloud.config.client.runtime.credentials.SpringCloudClientBasicAuthCredentialsProvider;
import io.quarkus.spring.cloud.config.client.runtime.credentials.SpringCloudConfigClientOidcCredentialsProvider;
import io.smallrye.mutiny.Uni;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyStoreOptionsBase;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class VertxSpringCloudConfigGateway implements SpringCloudConfigClientGateway {

    private static final Logger log = Logger.getLogger(VertxSpringCloudConfigGateway.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String PKS_12 = "PKS12";
    private static final String JKS = "JKS";

    private final SpringCloudConfigClientConfig springCloudConfigClientConfig;
    private final Vertx vertx;
    private final WebClient webClient;
    private final URI baseURI;
    private final SpringCloudClientCredentialsProvider authProvider;

    public VertxSpringCloudConfigGateway(SpringCloudConfigClientConfig springCloudConfigClientConfig, TlsConfig tlsConfig) {
        this.springCloudConfigClientConfig = springCloudConfigClientConfig;
        try {
            this.baseURI = determineBaseUri(springCloudConfigClientConfig);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Value: '" + springCloudConfigClientConfig.url
                    + "' of property 'quarkus.spring-cloud-config.url' is invalid", e);
        }
        this.vertx = Vertx.vertx();
        this.webClient = createHttpClient(vertx, springCloudConfigClientConfig, tlsConfig);
        this.authProvider = createAuthProvider(webClient, springCloudConfigClientConfig);
    }

    public WebClient createHttpClient(Vertx vertx, SpringCloudConfigClientConfig springCloudConfig,
            TlsConfig tlsConfig) {

        WebClientOptions webClientOptions = new WebClientOptions()
                .setConnectTimeout((int) springCloudConfig.connectionTimeout.toMillis())
                .setIdleTimeout((int) springCloudConfig.readTimeout.getSeconds());

        boolean trustAll = springCloudConfig.trustCerts || tlsConfig.trustAll;
        try {
            if (springCloudConfig.trustStore.isPresent()) {
                Path trustStorePath = springCloudConfig.trustStore.get();
                String type = determineStoreType(trustStorePath);
                KeyStoreOptionsBase storeOptions = storeOptions(trustStorePath, springCloudConfig.trustStorePassword,
                        createStoreOptions(type));
                if (isPfx(type)) {
                    webClientOptions.setPfxTrustOptions((PfxOptions) storeOptions);
                } else {
                    webClientOptions.setTrustStoreOptions((JksOptions) storeOptions);
                }
            } else if (trustAll) {
                skipVerify(webClientOptions);
            } else if (springCloudConfig.keyStore.isPresent()) {
                Path keyStorePath = springCloudConfig.keyStore.get();
                String type = determineStoreType(keyStorePath);
                KeyStoreOptionsBase storeOptions = storeOptions(keyStorePath, springCloudConfig.keyStorePassword,
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

    private SpringCloudClientCredentialsProvider createAuthProvider(WebClient webClient,
            SpringCloudConfigClientConfig cloudConfigClientConfig) {
        if (!cloudConfigClientConfig.usernameAndPasswordSet() && cloudConfigClientConfig.oidc != null) {
            return new SpringCloudConfigClientOidcCredentialsProvider(cloudConfigClientConfig, webClient);
        }
        return new SpringCloudClientBasicAuthCredentialsProvider(cloudConfigClientConfig);
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

    private URI determineBaseUri(SpringCloudConfigClientConfig springCloudConfigClientConfig) throws URISyntaxException {
        String url = springCloudConfigClientConfig.url;
        if (null == url || url.isEmpty()) {
            throw new IllegalArgumentException(
                    "The 'quarkus.spring-cloud-config.url' property cannot be empty");
        }
        if (url.endsWith("/")) {
            return new URI(url.substring(0, url.length() - 1));
        }
        return new URI(url);
    }

    private String finalURI(String applicationName, String profile) throws URISyntaxException {
        String path = baseURI.getPath();
        List<String> finalPathSegments = new ArrayList<String>();
        finalPathSegments.add(path);
        finalPathSegments.add(applicationName);
        finalPathSegments.add(profile);
        if (springCloudConfigClientConfig.label.isPresent()) {
            finalPathSegments.add(springCloudConfigClientConfig.label.get());
        }
        return finalPathSegments.stream().collect(Collectors.joining("/"));
    }

    @Override
    public Uni<Response> exchange(String applicationName, String profile) throws Exception {
        final String requestURI = finalURI(applicationName, profile);
        String finalURI = getFinalURI(applicationName, profile);
        HttpRequest<Buffer> request = webClient
                .get(getPort(baseURI), baseURI.getHost(), requestURI)
                .ssl(isHttps(baseURI))
                .putHeader("Accept", "application/json");

        authProvider.addAuthenticationInfo(request);
        for (Map.Entry<String, String> entry : springCloudConfigClientConfig.headers.entrySet()) {
            request.putHeader(entry.getKey(), entry.getValue());
        }
        log.debug("Attempting to read configuration from '" + finalURI + "'.");
        return request.send().map(r -> {
            if (r.statusCode() != 200) {
                throw new RuntimeException("Got unexpected HTTP response code " + r.statusCode()
                        + " from " + finalURI);
            } else {
                String bodyAsString = r.bodyAsString();
                if (bodyAsString.isEmpty()) {
                    throw new RuntimeException("Got empty HTTP response body " + finalURI);
                }
                try {
                    return OBJECT_MAPPER.readValue(bodyAsString, Response.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Got unexpected error " + e.getOriginalMessage());
                }
            }
        });
    }

    private boolean isHttps(URI uri) {
        return uri.getScheme().contains("https");
    }

    private int getPort(URI uri) {
        return uri.getPort() != -1 ? uri.getPort() : (isHttps(uri) ? 443 : 80);
    }

    private String getFinalURI(String applicationName, String profile) {
        String finalURI = baseURI.toString() + "/" + applicationName + "/" + profile;
        if (springCloudConfigClientConfig.label.isPresent()) {
            finalURI = "/" + springCloudConfigClientConfig.label.get();
        }
        return finalURI;
    }

    @Override
    public void close() {
        this.webClient.close();
        this.vertx.closeAndAwait();
    }

}
