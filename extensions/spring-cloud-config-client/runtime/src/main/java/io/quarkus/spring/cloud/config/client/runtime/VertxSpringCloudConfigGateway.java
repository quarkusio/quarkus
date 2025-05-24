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
import java.util.function.Function;

import io.quarkus.spring.cloud.config.client.runtime.eureka.DiscoveryService;
import io.quarkus.spring.cloud.config.client.runtime.eureka.EurekaClient;
import io.quarkus.spring.cloud.config.client.runtime.eureka.RandomEurekaInstanceSelector;
import io.quarkus.spring.cloud.config.client.runtime.eureka.EurekaResponseMapper;
import io.quarkus.spring.cloud.config.client.runtime.util.UrlUtility;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.ResettableSystemProperties;
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
    private final Function<SpringCloudConfigClientConfig, URI> baseUriSupplier;
    private final DiscoveryService discoveryService;

    public VertxSpringCloudConfigGateway(SpringCloudConfigClientConfig config) {
        this.config = config;
        this.vertx = createVertxInstance();
        this.webClient = createHttpClient(vertx, config);
        this.baseUriSupplier = determineBaseUri(config.discovery().enabled());
        this.discoveryService = new DiscoveryService(
                new EurekaClient(
                        webClient,
                        config.discovery().registryFetchIntervalSeconds(),
                        new EurekaResponseMapper(),
                        new RandomEurekaInstanceSelector()
                )
        );
    }

    private Vertx createVertxInstance() {
        // We must disable the async DNS resolver as it can cause issues when resolving the Vault instance.
        // This is done using the DISABLE_DNS_RESOLVER_PROP_NAME system property.
        // The DNS resolver used by vert.x is configured during the (synchronous) initialization.
        // So, we just need to disable the async resolver around the Vert.x instance creation.
        try (var resettableSystemProperties = ResettableSystemProperties.of(
                DISABLE_DNS_RESOLVER_PROP_NAME, "true")) {
            return Vertx.vertx(new VertxOptions());
        }
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

    private Function<SpringCloudConfigClientConfig, URI> determineBaseUri(boolean discoveryEnabled) {
        log.debug("Determining base URI for Spring Cloud Config Server");
        if (discoveryEnabled) {
            log.debug("Using 'discovery' mode to determine base URI");
            return getDiscoverUriSupplier();
        } else {
            return getUriSupplier();
        }
    }

    private Function<SpringCloudConfigClientConfig, URI> getUriSupplier() {
        return springCloudConfigClientConfig -> {
            log.debug("Attempting to obtain configuration from the Spring Cloud Config Server at " + config.url());
            String url = springCloudConfigClientConfig.url();
            try {
                validate(url);
                return UrlUtility.toURI(url);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Value: '" + springCloudConfigClientConfig.url()
                        + "' of property 'quarkus.spring-cloud-config.url' is invalid", e);
            }
        };
    }

    private void validate(String url) {
        if (null == url || url.isEmpty()) {
            throw new IllegalArgumentException(
                    "The 'quarkus.spring-cloud-config.url' property cannot be empty");
        }
    }

    private Function<SpringCloudConfigClientConfig, URI> getDiscoverUriSupplier() {
        return springCloudConfigClientConfig -> {
            String discoveredUrl = discoveryService.discover(springCloudConfigClientConfig);
            try {
                return UrlUtility.toURI(discoveredUrl);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private ConfigServerUrl toConfigServerUrl(String applicationName, String profile) {
        URI baseURI = baseUriSupplier.apply(config);
        String path = baseURI.getPath();
        List<String> finalPathSegments = new ArrayList<>();
        finalPathSegments.add(path);
        finalPathSegments.add(applicationName);
        finalPathSegments.add(profile);
        if (config.label().isPresent()) {
            finalPathSegments.add(config.label().get());
        }
        return new ConfigServerUrl(baseURI, UrlUtility.getPort(baseURI), baseURI.getHost(), String.join("/", finalPathSegments));
    }

    @Override
    public Uni<Response> exchange(String applicationName, String profile) {
        final ConfigServerUrl requestURI = toConfigServerUrl(applicationName, profile);
        HttpRequest<Buffer> request = webClient
                .get(requestURI.port(), requestURI.host(), requestURI.completeURLString())
                .ssl(UrlUtility.isHttps(requestURI.baseURI()))
                .putHeader("Accept", "application/json");
        if (config.usernameAndPasswordSet()) {
            request.basicAuthentication(config.username().get(), config.password().get());
        }
        for (Map.Entry<String, String> entry : config.headers().entrySet()) {
            request.putHeader(entry.getKey(), entry.getValue());
        }
        log.debug("Attempting to read configuration from '" + requestURI.completeURLString() + "'.");
        return request.send().map(r -> {
            log.debug("Received HTTP response code '" + r.statusCode() + "'");
            if (r.statusCode() != 200) {
                throw new RuntimeException("Got unexpected HTTP response code " + r.statusCode()
                        + " from " + requestURI.completeURLString());
            } else {
                String bodyAsString = r.bodyAsString();
                if (bodyAsString.isEmpty()) {
                    throw new RuntimeException("Got empty HTTP response body " + requestURI.completeURLString());
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

    @Override
    public void close() {
        this.webClient.close();
        this.vertx.closeAndAwait();
    }
}
