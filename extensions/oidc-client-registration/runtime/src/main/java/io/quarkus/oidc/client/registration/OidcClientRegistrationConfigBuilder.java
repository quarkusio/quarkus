package io.quarkus.oidc.client.registration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder;
import io.smallrye.config.ConfigInstanceBuilder;

/**
 * The {@link OidcClientRegistrationConfig} builder. This builder is not thread safe.
 */
public final class OidcClientRegistrationConfigBuilder extends OidcCommonConfigBuilder<OidcClientRegistrationConfigBuilder> {

    private static OidcClientRegistrationConfig create(OidcClientRegistrationConfigBuilder builder) {
        return commonConfig(OidcClientRegistrationConfig.class, builder)
                .with(OidcClientRegistrationConfig::id, builder.id)
                .with(OidcClientRegistrationConfig::registrationEnabled, builder.registrationEnabled)
                .with(OidcClientRegistrationConfig::registerEarly, builder.registerEarly)
                .with(OidcClientRegistrationConfig::initialToken, builder.initialToken)
                .with(OidcClientRegistrationConfig::metadata, builder.metadata)
                .build();
    }

    /**
     * {@link OidcClientRegistrationConfig} with documented defaults.
     * Cached here so that we avoid building the SmallRye Config again and again when no-args builder constructors
     * are used.
     */
    private static volatile OidcClientRegistrationConfig configWithDefaults = null;

    private Optional<String> id;
    private boolean registrationEnabled;
    private boolean registerEarly;
    private Optional<String> initialToken;
    private OidcClientRegistrationConfig.Metadata metadata;

    /**
     * Creates {@link OidcClientRegistrationConfig} builder populated with documented default values.
     */
    public OidcClientRegistrationConfigBuilder() {
        this(getConfigWithDefaults());
    }

    /**
     * Creates {@link OidcClientRegistrationConfig} builder populated with {@code config} values.
     */
    public OidcClientRegistrationConfigBuilder(OidcClientRegistrationConfig config) {
        super(Objects.requireNonNull(config));
        this.id = config.id();
        this.registrationEnabled = config.registrationEnabled();
        this.registerEarly = config.registerEarly();
        this.initialToken = config.initialToken();
        this.metadata = config.metadata();
    }

    @Override
    protected OidcClientRegistrationConfigBuilder getBuilder() {
        return this;
    }

    /**
     * @param id {@link OidcClientRegistrationConfig#id()}
     * @return this builder
     */
    public OidcClientRegistrationConfigBuilder id(String id) {
        this.id = Optional.ofNullable(id);
        return this;
    }

    /**
     * @param registrationEnabled {@link OidcClientRegistrationConfig#registrationEnabled()}
     * @return this builder
     */
    public OidcClientRegistrationConfigBuilder registrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
        return this;
    }

    /**
     * @param registerEarly {@link OidcClientRegistrationConfig#registerEarly()}
     * @return this builder
     */
    public OidcClientRegistrationConfigBuilder registerEarly(boolean registerEarly) {
        this.registerEarly = registerEarly;
        return this;
    }

    /**
     * @param initialToken {@link OidcClientRegistrationConfig#initialToken()}
     * @return this builder
     */
    public OidcClientRegistrationConfigBuilder initialToken(String initialToken) {
        this.initialToken = Optional.ofNullable(initialToken);
        return this;
    }

    /**
     * @param redirectUri {@link OidcClientRegistrationConfig.Metadata#redirectUri()}
     * @return this builder
     */
    public OidcClientRegistrationConfigBuilder metadata(String redirectUri) {
        return metadata().redirectUri(redirectUri).end();
    }

    /**
     * @param clientName {@link OidcClientRegistrationConfig.Metadata#clientName()}
     * @param redirectUri {@link OidcClientRegistrationConfig.Metadata#redirectUri()}
     * @return this builder
     */
    public OidcClientRegistrationConfigBuilder metadata(String clientName, String redirectUri) {
        return metadata().clientName(clientName).redirectUri(redirectUri).end();
    }

    /**
     * @param metadata {@link OidcClientRegistrationConfig#metadata()}
     * @return this builder
     */
    public OidcClientRegistrationConfigBuilder metadata(OidcClientRegistrationConfig.Metadata metadata) {
        this.metadata = Objects.requireNonNull(metadata);
        return this;
    }

    /**
     * Builder for {@link OidcClientRegistrationConfig#metadata()}.
     *
     * @return MetadataBuilder
     */
    public MetadataBuilder metadata() {
        return new MetadataBuilder(this);
    }

    /**
     * @return OidcClientRegistrationConfig
     */
    public OidcClientRegistrationConfig build() {
        return create(this);
    }

    public static final class MetadataBuilder {

        private Optional<String> clientName = Optional.empty();
        private Optional<String> redirectUri = Optional.empty();
        private Optional<String> postLogoutUri = Optional.empty();
        private final Map<String, String> extraProps = new HashMap<>();
        private final OidcClientRegistrationConfigBuilder configBuilder;

        public MetadataBuilder(OidcClientRegistrationConfigBuilder configBuilder) {
            this.configBuilder = configBuilder;
        }

        public MetadataBuilder() {
            this(new OidcClientRegistrationConfigBuilder());
        }

        public OidcClientRegistrationConfig.Metadata build() {
            return ConfigInstanceBuilder.forInterface(OidcClientRegistrationConfig.Metadata.class)
                    .with(OidcClientRegistrationConfig.Metadata::clientName, clientName)
                    .with(OidcClientRegistrationConfig.Metadata::redirectUri, redirectUri)
                    .with(OidcClientRegistrationConfig.Metadata::postLogoutUri, postLogoutUri)
                    .with(OidcClientRegistrationConfig.Metadata::extraProps, extraProps)
                    .build();
        }

        public OidcClientRegistrationConfigBuilder end() {
            Objects.requireNonNull(configBuilder);
            this.configBuilder.metadata = build();
            return configBuilder;
        }

        /**
         * @param clientName {@link OidcClientRegistrationConfig.Metadata#clientName()}
         * @return this builder
         */
        public MetadataBuilder clientName(String clientName) {
            this.clientName = Optional.ofNullable(clientName);
            return this;
        }

        /**
         * @param redirectUri {@link OidcClientRegistrationConfig.Metadata#redirectUri()}
         * @return this builder
         */
        public MetadataBuilder redirectUri(String redirectUri) {
            this.redirectUri = Optional.ofNullable(redirectUri);
            return this;
        }

        /**
         * @param postLogoutUri {@link OidcClientRegistrationConfig.Metadata#postLogoutUri()}
         * @return this builder
         */
        public MetadataBuilder postLogoutUri(String postLogoutUri) {
            this.postLogoutUri = Optional.ofNullable(postLogoutUri);
            return this;
        }

        /**
         * Adds extra property to {@link OidcClientRegistrationConfig.Metadata#extraProps()}.
         *
         * @param key extra property key; must not be null
         * @param value extra property value; must not be null
         * @return this builder
         */
        public MetadataBuilder extraProperty(String key, String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            this.extraProps.put(key, value);
            return this;
        }

        /**
         * @param extraProps {@link OidcClientRegistrationConfig.Metadata#extraProps()}; must not be null
         * @return this builder
         */
        public MetadataBuilder extraProps(Map<String, String> extraProps) {
            Objects.requireNonNull(extraProps);
            this.extraProps.putAll(extraProps);
            return this;
        }
    }

    private static OidcClientRegistrationConfig getConfigWithDefaults() {
        if (configWithDefaults == null) {
            configWithDefaults = ConfigInstanceBuilder.forInterface(OidcClientRegistrationConfig.class).build();
        }
        return configWithDefaults;
    }
}
