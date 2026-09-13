package io.quarkus.spring.cloud.config.client.runtime;

import static io.vertx.core.impl.SysProps.DISABLE_DNS_RESOLVER;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ResettableSystemProperties;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.spring.cloud.config.client.runtime.eureka.DiscoveryService;
import io.quarkus.spring.cloud.config.client.runtime.eureka.EurekaClient;
import io.quarkus.spring.cloud.config.client.runtime.eureka.EurekaResponseMapper;
import io.quarkus.spring.cloud.config.client.runtime.eureka.RandomEurekaInstanceSelector;
import io.quarkus.spring.cloud.config.client.runtime.util.UrlUtility;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyStoreOptionsBase;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class VertxSpringCloudConfigGateway implements SpringCloudConfigClientGateway {

    private static final Logger log = Logger.getLogger(VertxSpringCloudConfigGateway.class);

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

    private static final String PKS_12 = "PKS12";
    private static final String JKS = "JKS";

    private final SpringCloudConfigClientConfig config;
    private final Vertx vertx;
    private final WebClient webClient;
    private final ConfigServerBaseUrlProvider configServerBaseUrlProvider;

    public VertxSpringCloudConfigGateway(SpringCloudConfigClientConfig config) {
        this.config = config;
        this.vertx = createVertxInstance();
        this.webClient = createHttpClient(vertx, config);
        this.configServerBaseUrlProvider = createConfigServerProvider(config);
    }

    private ConfigServerBaseUrlProvider createConfigServerProvider(SpringCloudConfigClientConfig config) {
        if (!config.discovery().isPresent() || (!config.discovery().get().enabled())) {
            return new DirectConfigServerBaseUrlProvider(config);
        }
        DiscoveryService discoveryService = createDiscoveryService(config.discovery().get());
        return new DiscoveryConfigServerBaseUrlProvider(discoveryService, config);
    }

    private DiscoveryService createDiscoveryService(SpringCloudConfigClientConfig.DiscoveryConfig config) {
        EurekaClient eurekaClient = createEurekaClient(config.eurekaConfig().get());
        return new DiscoveryService(eurekaClient);
    }

    private EurekaClient createEurekaClient(SpringCloudConfigClientConfig.DiscoveryConfig.EurekaConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Eureka configuration is required");
        }
        Duration fetchInterval = config.registryFetchIntervalSeconds();
        EurekaResponseMapper responseMapper = new EurekaResponseMapper();
        RandomEurekaInstanceSelector instanceSelector = new RandomEurekaInstanceSelector();

        return new EurekaClient(
                webClient,
                fetchInterval,
                responseMapper,
                instanceSelector);
    }

    private Vertx createVertxInstance() {
        // We must disable the async DNS resolver as it can cause issues when resolving the Vault instance.
        // This is done using the DISABLE_DNS_RESOLVER system property.
        // The DNS resolver used by vert.x is configured during the (synchronous) initialization.
        // So, we just need to disable the async resolver around the Vert.x instance creation.
        try (var resettableSystemProperties = ResettableSystemProperties.of(
                DISABLE_DNS_RESOLVER.name, "true")) {
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
                    webClientOptions.setTrustOptions(storeOptions);
                } else {
                    webClientOptions.setTrustOptions(storeOptions);
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
                    webClientOptions.setKeyCertOptions(storeOptions);
                } else {
                    webClientOptions.setKeyCertOptions(storeOptions);
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

    private ConfigServerUrl toConfigServerUrl(String applicationName, String profile) {
        URI baseURI = configServerBaseUrlProvider.get();
        String path = baseURI.getPath();
        List<String> finalPathSegments = new ArrayList<>();
        finalPathSegments.add(path);
        finalPathSegments.add(applicationName);
        finalPathSegments.add(profile);
        if (config.label().isPresent()) {
            finalPathSegments.add(config.label().get());
        }
        return new ConfigServerUrl(baseURI, UrlUtility.getPort(baseURI), baseURI.getHost(),
                String.join("/", finalPathSegments));
    }

    @Override
    public Uni<Response> exchange(String applicationName, String profile) {
        if (config.oidc().isPresent()) {
            return acquireToken(config.oidc().get()).flatMap(token -> exchange(applicationName, profile, token));
        }
        return exchange(applicationName, profile, null);
    }

    /**
     * Obtains a bearer token from the token endpoint with the configured grant. The token is requested for every
     * configuration fetch, which happens at startup and, when enabled, at each periodic refresh, so no caching is
     * needed.
     */
    private Uni<String> acquireToken(SpringCloudConfigClientConfig.OidcConfig oidc) {
        URI tokenUri = URI.create(oidc.tokenUrl());
        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.set("grant_type", oidc.grantType());
        if ("password".equals(oidc.grantType())) {
            form.set("username", oidc.username().orElseThrow(() -> new IllegalArgumentException(
                    "quarkus.spring-cloud-config.oidc.username is required for the password grant type")));
            form.set("password", oidc.password().orElseThrow(() -> new IllegalArgumentException(
                    "quarkus.spring-cloud-config.oidc.password is required for the password grant type")));
        }
        oidc.scope().ifPresent(scope -> form.set("scope", scope));
        HttpRequest<Buffer> request = webClient
                .post(UrlUtility.getPort(tokenUri), tokenUri.getHost(), tokenUri.getRawPath())
                .ssl(UrlUtility.isHttps(tokenUri))
                .putHeader("Accept", "application/json");
        if (oidc.clientSecret().isPresent()) {
            request.basicAuthentication(oidc.clientId(), oidc.clientSecret().get());
        } else {
            form.set("client_id", oidc.clientId());
        }
        log.debug("Requesting a bearer token from '" + oidc.tokenUrl() + "'.");
        return request.sendForm(form).map(r -> {
            if (r.statusCode() != 200) {
                throw new RuntimeException("Got unexpected HTTP response code " + r.statusCode()
                        + " from the token endpoint " + oidc.tokenUrl());
            }
            try {
                String accessToken = OBJECT_MAPPER.readTree(r.bodyAsString()).path("access_token").asString(null);
                if (accessToken == null || accessToken.isEmpty()) {
                    throw new RuntimeException("The response of the token endpoint " + oidc.tokenUrl()
                            + " does not contain an access_token");
                }
                return accessToken;
            } catch (JacksonException e) {
                throw new RuntimeException("Got unexpected error " + e.getOriginalMessage()
                        + " when reading the response of the token endpoint " + oidc.tokenUrl());
            }
        });
    }

    private Uni<Response> exchange(String applicationName, String profile, String bearerToken) {
        final ConfigServerUrl requestURI = toConfigServerUrl(applicationName, profile);
        HttpRequest<Buffer> request = webClient
                .get(requestURI.port(), requestURI.host(), requestURI.completeURLString())
                .ssl(UrlUtility.isHttps(requestURI.baseURI()))
                .putHeader("Accept", "application/json");
        if (config.usernameAndPasswordSet()) {
            request.basicAuthentication(config.username().get(), config.password().get());
        }
        if (bearerToken != null) {
            request.bearerTokenAuthentication(bearerToken);
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
                } catch (JacksonException e) {
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
