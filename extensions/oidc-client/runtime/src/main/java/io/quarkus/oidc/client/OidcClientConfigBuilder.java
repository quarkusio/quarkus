package io.quarkus.oidc.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.oidc.client.runtime.OidcClientConfig;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant;
import io.quarkus.oidc.client.runtime.OidcClientsConfig;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Builder for the {@link io.quarkus.oidc.client.runtime.OidcClientConfig}. This builder is not thread-safe.
 */
public final class OidcClientConfigBuilder extends OidcClientCommonConfigBuilder<OidcClientConfigBuilder> {

    private static class OidcClientConfigImpl extends OidcClientCommonConfigImpl implements OidcClientConfig {

        private final Map<String, String> headers;
        private final boolean earlyTokensAcquisition;
        private final Map<String, Map<String, String>> grantOptions;
        private final Grant grant;
        private final boolean absoluteExpiresIn;
        private final Optional<Duration> accessTokenExpiresIn;
        private final Optional<Duration> accessTokenExpirySkew;
        private final Optional<Duration> refreshTokenTimeSkew;
        private final Optional<List<String>> scopes;
        private final boolean clientEnabled;
        private final Optional<String> id;
        private final Optional<Duration> refreshInterval;;

        private OidcClientConfigImpl(OidcClientConfigBuilder builder) {
            super(builder);
            this.headers = Map.copyOf(builder.headers);
            this.earlyTokensAcquisition = builder.earlyTokensAcquisition;
            this.grantOptions = Map.copyOf(builder.grantOptions);
            this.grant = builder.grant;
            this.absoluteExpiresIn = builder.absoluteExpiresIn;
            this.accessTokenExpiresIn = builder.accessTokenExpiresIn;
            this.accessTokenExpirySkew = builder.accessTokenExpirySkew;
            this.refreshTokenTimeSkew = builder.refreshTokenTimeSkew;
            this.scopes = builder.scopes.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(builder.scopes));
            this.clientEnabled = builder.clientEnabled;
            this.id = builder.id;
            this.refreshInterval = builder.refreshInterval;
        }

        @Override
        public Optional<String> id() {
            return id;
        }

        @Override
        public boolean clientEnabled() {
            return clientEnabled;
        }

        @Override
        public Optional<List<String>> scopes() {
            return scopes;
        }

        @Override
        public Optional<Duration> refreshTokenTimeSkew() {
            return refreshTokenTimeSkew;
        }

        @Override
        public Optional<Duration> accessTokenExpiresIn() {
            return accessTokenExpiresIn;
        }

        @Override
        public Optional<Duration> accessTokenExpirySkew() {
            return accessTokenExpirySkew;
        }

        @Override
        public boolean absoluteExpiresIn() {
            return absoluteExpiresIn;
        }

        @Override
        public Grant grant() {
            return grant;
        }

        @Override
        public Map<String, Map<String, String>> grantOptions() {
            return grantOptions;
        }

        @Override
        public boolean earlyTokensAcquisition() {
            return earlyTokensAcquisition;
        }

        @Override
        public Map<String, String> headers() {
            return headers;
        }

        @Override
        public Optional<Duration> refreshInterval() {
            return refreshInterval;
        }
    }

    /**
     * {@link OidcClientConfig} with documented defaults.
     * Cached here so that we avoid building the SmallRye Config again and again when no-args builder constructors
     * are used.
     */
    private static volatile OidcClientConfig configWithDefaults = null;

    private final Map<String, String> headers = new HashMap<>();
    private boolean earlyTokensAcquisition;
    private final Map<String, Map<String, String>> grantOptions = new HashMap<>();
    private final List<String> scopes = new ArrayList<>();
    private Grant grant;
    private boolean absoluteExpiresIn;
    private Optional<Duration> accessTokenExpiresIn;
    private Optional<Duration> accessTokenExpirySkew;
    private Optional<Duration> refreshTokenTimeSkew;
    private boolean clientEnabled;
    private Optional<String> id;
    private Optional<Duration> refreshInterval;

    /**
     * Creates {@link OidcClientConfigBuilder} builder populated with documented default values.
     */
    public OidcClientConfigBuilder() {
        this(getConfigWithDefaults());
    }

    /**
     * @param config created either by this builder or SmallRye Config; config methods must never return null
     */
    public OidcClientConfigBuilder(OidcClientConfig config) {
        super(Objects.requireNonNull(config));
        this.headers.putAll(config.headers());
        this.earlyTokensAcquisition = config.earlyTokensAcquisition();
        this.grantOptions.putAll(config.grantOptions());
        this.grant = config.grant();
        this.absoluteExpiresIn = config.absoluteExpiresIn();
        this.accessTokenExpiresIn = config.accessTokenExpiresIn();
        this.accessTokenExpirySkew = config.accessTokenExpirySkew();
        this.refreshTokenTimeSkew = config.refreshTokenTimeSkew();
        this.clientEnabled = config.clientEnabled();
        this.id = config.id();
        this.refreshInterval = config.refreshInterval();
        if (config.scopes().isPresent()) {
            this.scopes.addAll(config.scopes().get());
        }
    }

    @Override
    protected OidcClientConfigBuilder getBuilder() {
        return this;
    }

    /**
     * Adds new headers to the {@link OidcClientConfig#headers()} already set.
     *
     * @param headerName header name
     * @param headerValue header value
     * @return this builder
     */
    public OidcClientConfigBuilder headers(String headerName, String headerValue) {
        Objects.requireNonNull(headerName);
        Objects.requireNonNull(headerValue);
        this.headers.put(headerName, headerValue);
        return this;
    }

    /**
     * Adds new headers to the headers already set.
     *
     * @param headers {@link OidcClientConfig#headers()}
     * @return this builder
     */
    public OidcClientConfigBuilder headers(Map<String, String> headers) {
        Objects.requireNonNull(headers);
        this.headers.putAll(headers);
        return this;
    }

    /**
     * @param earlyTokensAcquisition {@link OidcClientConfig#earlyTokensAcquisition()}
     * @return this builder
     */
    public OidcClientConfigBuilder earlyTokensAcquisition(boolean earlyTokensAcquisition) {
        this.earlyTokensAcquisition = earlyTokensAcquisition;
        return this;
    }

    /**
     * Adds {@link OidcClientConfig#grantOptions()}.
     *
     * @return this builder
     */
    public OidcClientConfigBuilder grantOptions(String grantName, Map<String, String> options) {
        Objects.requireNonNull(grantName);
        Objects.requireNonNull(options);
        this.grantOptions.computeIfAbsent(grantName, k -> new HashMap<>()).putAll(options);
        return this;
    }

    /**
     * Adds {@link OidcClientConfig#grantOptions()}.
     *
     * @return this builder
     */
    public OidcClientConfigBuilder grantOptions(String grantName, String key, String value) {
        Objects.requireNonNull(grantName);
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        this.grantOptions.computeIfAbsent(grantName, k -> new HashMap<>()).put(key, value);
        return this;
    }

    /**
     * @param grantOptions {@link OidcClientConfig#grantOptions()}
     * @return this builder
     */
    public OidcClientConfigBuilder grantOptions(Map<String, Map<String, String>> grantOptions) {
        Objects.requireNonNull(grantOptions);
        this.grantOptions.putAll(grantOptions);
        return this;
    }

    /**
     * @param absoluteExpiresIn {@link OidcClientConfig#absoluteExpiresIn()}
     * @return this builder
     */
    public OidcClientConfigBuilder absoluteExpiresIn(boolean absoluteExpiresIn) {
        this.absoluteExpiresIn = absoluteExpiresIn;
        return this;
    }

    /**
     * @param accessTokenExpiresIn {@link OidcClientConfig#accessTokenExpiresIn()}
     * @return this builder
     */
    public OidcClientConfigBuilder accessTokenExpiresIn(Duration accessTokenExpiresIn) {
        this.accessTokenExpiresIn = Optional.ofNullable(accessTokenExpiresIn);
        return this;
    }

    /**
     * @param accessTokenExpirySkew {@link OidcClientConfig#accessTokenExpirySkew()}
     * @return this builder
     */
    public OidcClientConfigBuilder accessTokenExpirySkew(Duration accessTokenExpirySkew) {
        this.accessTokenExpirySkew = Optional.ofNullable(accessTokenExpirySkew);
        return this;
    }

    /**
     * @param refreshTokenTimeSkew {@link OidcClientConfig#refreshTokenTimeSkew()}
     * @return this builder
     */
    public OidcClientConfigBuilder refreshTokenTimeSkew(Duration refreshTokenTimeSkew) {
        this.refreshTokenTimeSkew = Optional.ofNullable(refreshTokenTimeSkew);
        return this;
    }

    /**
     * Adds scopes to the {@link OidcClientConfig#scopes()}.
     *
     * @param scopes {@link OidcClientConfig#scopes()}
     * @return this builder
     */
    public OidcClientConfigBuilder scopes(List<String> scopes) {
        Objects.requireNonNull(scopes);
        this.scopes.addAll(scopes);
        return this;
    }

    /**
     * Adds scopes to the {@link OidcClientConfig#scopes()}.
     *
     * @param scopes {@link OidcClientConfig#scopes()}
     * @return this builder
     */
    public OidcClientConfigBuilder scopes(String... scopes) {
        Objects.requireNonNull(scopes);
        this.scopes.addAll(Arrays.asList(scopes));
        return this;
    }

    /**
     * @param clientEnabled {@link OidcClientConfig#clientEnabled()}
     * @return this builder
     */
    public OidcClientConfigBuilder clientEnabled(boolean clientEnabled) {
        this.clientEnabled = clientEnabled;
        return this;
    }

    /**
     * @param id {@link OidcClientConfig#id()}
     * @return this builder
     */
    public OidcClientConfigBuilder id(String id) {
        this.id = Optional.ofNullable(id);
        return this;
    }

    /**
     * @param grant {@link OidcClientConfig#grant()} created either with {@link GrantBuilder} or SmallRye Config
     * @return this builder
     */
    public OidcClientConfigBuilder grant(Grant grant) {
        this.grant = Objects.requireNonNull(grant);
        return this;
    }

    /**
     * @param type {@link Grant#type()}
     * @return this builder
     */
    public OidcClientConfigBuilder grant(Grant.Type type) {
        return grant().type(type).end();
    }

    /**
     * Creates {@link OidcClientConfig#grant()} builder.
     *
     * @return GrantBuilder
     */
    public GrantBuilder grant() {
        return new GrantBuilder(this);
    }

    public OidcClientConfigBuilder refreshInterval(Duration refreshInterval) {
        this.refreshInterval = Optional.ofNullable(refreshInterval);
        return this;
    }

    /**
     * @return OidcClientConfig
     */
    public OidcClientConfig build() {
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be empty");
        }
        return new OidcClientConfigImpl(this);
    }

    public static final class GrantBuilder {

        private record GrantImpl(Type type, String accessTokenProperty, String refreshTokenProperty, String expiresInProperty,
                String refreshExpiresInProperty) implements Grant {

        }

        private final OidcClientConfigBuilder builder;
        private Grant.Type type;
        private String accessTokenProperty;
        private String refreshTokenProperty;
        private String expiresInProperty;
        private String refreshExpiresInProperty;

        public GrantBuilder() {
            this(new OidcClientConfigBuilder());
        }

        public GrantBuilder(OidcClientConfigBuilder builder) {
            this.builder = builder;
            this.type = builder.grant.type();
            this.accessTokenProperty = builder.grant.accessTokenProperty();
            this.refreshTokenProperty = builder.grant.refreshTokenProperty();
            this.expiresInProperty = builder.grant.expiresInProperty();
            this.refreshExpiresInProperty = builder.grant.refreshExpiresInProperty();
        }

        /**
         * @param type {@link Grant#type()}
         * @return this builder
         */
        public GrantBuilder type(Grant.Type type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * @param refreshTokenProperty {@link Grant#refreshTokenProperty()}
         * @return this builder
         */
        public GrantBuilder refreshTokenProperty(String refreshTokenProperty) {
            this.refreshTokenProperty = Objects.requireNonNull(refreshTokenProperty);
            return this;
        }

        /**
         * @param expiresInProperty {@link Grant#expiresInProperty()}
         * @return this builder
         */
        public GrantBuilder expiresInProperty(String expiresInProperty) {
            this.expiresInProperty = Objects.requireNonNull(expiresInProperty);
            return this;
        }

        /**
         * @param refreshExpiresInProperty {@link Grant#refreshExpiresInProperty()}
         * @return this builder
         */
        public GrantBuilder refreshExpiresInProperty(String refreshExpiresInProperty) {
            this.refreshExpiresInProperty = Objects.requireNonNull(refreshExpiresInProperty);
            return this;
        }

        /**
         * @param accessTokenProperty {@link Grant#accessTokenProperty()}
         * @return this builder
         */
        public GrantBuilder accessTokenProperty(String accessTokenProperty) {
            this.accessTokenProperty = Objects.requireNonNull(accessTokenProperty);
            return this;
        }

        public OidcClientConfigBuilder end() {
            Objects.requireNonNull(builder);
            return builder.grant(build());
        }

        public Grant build() {
            return new GrantImpl(type, accessTokenProperty, refreshTokenProperty, expiresInProperty, refreshExpiresInProperty);
        }
    }

    private static OidcClientConfig getConfigWithDefaults() {
        if (configWithDefaults == null) {
            final OidcClientsConfig clientsConfig = new SmallRyeConfigBuilder()
                    .addDiscoveredConverters()
                    .withMapping(OidcClientsConfig.class)
                    .build()
                    .getConfigMapping(OidcClientsConfig.class);
            configWithDefaults = OidcClientsConfig.getDefaultClient(clientsConfig);
        }
        return configWithDefaults;
    }
}
