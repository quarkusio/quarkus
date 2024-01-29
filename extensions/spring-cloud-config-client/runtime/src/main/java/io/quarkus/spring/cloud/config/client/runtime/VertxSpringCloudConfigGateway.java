package io.quarkus.spring.cloud.config.client.runtime;

import static io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.util.ClassPathUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.VertxOptions;
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

    private final SpringCloudConfigClientConfig config;
    private final Vertx vertx;
    private final WebClient webClient;
    private final URI baseURI;

    public VertxSpringCloudConfigGateway(SpringCloudConfigClientConfig config) {
        this.config = config;
        try {
            this.baseURI = determineBaseUri(config);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Value: '" + config.url()
                    + "' of property 'quarkus.spring-cloud-config.url' is invalid", e);
        }
        this.vertx = createVertxInstance();
        this.webClient = createHttpClient(vertx, config);
    }

    private Vertx createVertxInstance() {
        // We must disable the async DNS resolver as it can cause issues when resolving the Vault instance.
        // This is done using the DISABLE_DNS_RESOLVER_PROP_NAME system property.
        // The DNS resolver used by vert.x is configured during the (synchronous) initialization.
        // So, we just need to disable the async resolver around the Vert.x instance creation.
        String originalValue = System.getProperty(DISABLE_DNS_RESOLVER_PROP_NAME);
        Vertx vertx;
        try {
            System.setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, "true");
            vertx = Vertx.vertx(new VertxOptions());
        } finally {
            // Restore the original value
            if (originalValue == null) {
                System.clearProperty(DISABLE_DNS_RESOLVER_PROP_NAME);
            } else {
                System.setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, originalValue);
            }
        }
        return vertx;
    }

    public static WebClient createHttpClient(Vertx vertx, SpringCloudConfigClientConfig config) {
        WebClientOptions webClientOptions = new WebClientOptions()
                .setConnectTimeout((int) config.connectionTimeout().toMillis())
                .setIdleTimeout((int) config.readTimeout().getSeconds());

        try {
            if (config.trustStore().isPresent()) {
                Path trustStorePath = config.trustStore().get();
                String type = determineStoreType(trustStorePath);
                KeyStoreOptionsBase storeOptions = storeOptions(trustStorePath, config.trustStorePassword(),
                        createStoreOptions(type));
                if (isPfx(type)) {
                    webClientOptions.setPfxTrustOptions((PfxOptions) storeOptions);
                } else {
                    webClientOptions.setTrustStoreOptions((JksOptions) storeOptions);
                }
            } else if (config.trustCerts()) {
                skipVerify(webClientOptions);
            }
            if (config.keyStore().isPresent()) {
                Path keyStorePath = config.keyStore().get();
                String type = determineStoreType(keyStorePath);
                KeyStoreOptionsBase storeOptions = storeOptions(keyStorePath, config.keyStorePassword(),
                        createStoreOptions(type));
                if (isPfx(type)) {
                    webClientOptions.setPfxKeyCertOptions((PfxOptions) storeOptions);
                } else {
                    webClientOptions.setKeyStoreOptions((JksOptions) storeOptions);
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
                .getResourceAsStream(ClassPathUtils.toResourceName(keyStorePath));
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
        return inputStream.readAllBytes();
    }

    private URI determineBaseUri(SpringCloudConfigClientConfig springCloudConfigClientConfig) throws URISyntaxException {
        String url = springCloudConfigClientConfig.url();
        if (null == url || url.isEmpty()) {
            throw new IllegalArgumentException(
                    "The 'quarkus.spring-cloud-config.url' property cannot be empty");
        }
        if (url.endsWith("/")) {
            return new URI(url.substring(0, url.length() - 1));
        }
        return new URI(url);
    }

    private String finalURI(String applicationName, String profile) {
        String path = baseURI.getPath();
        List<String> finalPathSegments = new ArrayList<String>();
        finalPathSegments.add(path);
        finalPathSegments.add(applicationName);
        finalPathSegments.add(profile);
        if (config.label().isPresent()) {
            finalPathSegments.add(config.label().get());
        }
        return String.join("/", finalPathSegments);
    }

    @Override
    public Uni<Response> exchange(String applicationName, String profile) {
        final String requestURI = finalURI(applicationName, profile);
        String finalURI = getFinalURI(applicationName, profile);
        HttpRequest<Buffer> request = webClient
                .get(getPort(baseURI), baseURI.getHost(), requestURI)
                .ssl(isHttps(baseURI))
                .putHeader("Accept", "application/json");
        if (config.usernameAndPasswordSet()) {
            request.basicAuthentication(config.username().get(), config.password().get());
        }
        for (Map.Entry<String, String> entry : config.headers().entrySet()) {
            request.putHeader(entry.getKey(), entry.getValue());
        }
        log.debug("Attempting to read configuration from '" + finalURI + "'.");
        return request.send().map(r -> {
            log.debug("Received HTTP response code '" + r.statusCode() + "'");
            if (r.statusCode() != 200) {
                throw new RuntimeException("Got unexpected HTTP response code " + r.statusCode()
                        + " from " + finalURI);
            } else {
                String bodyAsString = r.bodyAsString();
                if (bodyAsString.isEmpty()) {
                    throw new RuntimeException("Got empty HTTP response body " + finalURI);
                }
                try {
                    log.debug("Attempting to deserialize response");
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
        if (config.label().isPresent()) {
            finalURI += "/" + config.label().get();
        }
        return finalURI;
    }

    @Override
    public void close() {
        this.webClient.close();
        this.vertx.closeAndAwait();
    }
}
