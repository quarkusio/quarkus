package io.quarkus.oidc.common.runtime.config;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Jwt;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Jwt.Source;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Provider;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret.Method;
import io.smallrye.config.SmallRyeConfigBuilder;

public abstract class OidcClientCommonConfigBuilder<T> extends OidcCommonConfigBuilder<T> {

    protected static class OidcClientCommonConfigImpl extends OidcCommonConfigImpl implements OidcClientCommonConfig {

        private final Optional<String> tokenPath;
        private final Optional<String> revokePath;
        private final Optional<String> clientId;
        private final Optional<String> clientName;
        private final Credentials credentials;

        protected OidcClientCommonConfigImpl(OidcClientCommonConfigBuilder<?> builder) {
            super(builder);
            this.tokenPath = builder.tokenPath;
            this.revokePath = builder.revokePath;
            this.clientId = builder.clientId;
            this.clientName = builder.clientName;
            this.credentials = builder.credentials;
        }

        @Override
        public Optional<String> tokenPath() {
            return tokenPath;
        }

        @Override
        public Optional<String> revokePath() {
            return revokePath;
        }

        @Override
        public Optional<String> clientId() {
            return clientId;
        }

        @Override
        public Optional<String> clientName() {
            return clientName;
        }

        @Override
        public Credentials credentials() {
            return credentials;
        }
    }

    private Optional<String> tokenPath;
    private Optional<String> revokePath;
    private Optional<String> clientId;
    private Optional<String> clientName;
    private Credentials credentials;

    protected OidcClientCommonConfigBuilder(OidcClientCommonConfig oidcClientCommonConfig) {
        super(oidcClientCommonConfig);
        this.tokenPath = oidcClientCommonConfig.tokenPath();
        this.revokePath = oidcClientCommonConfig.revokePath();
        this.clientId = oidcClientCommonConfig.clientId();
        this.clientName = oidcClientCommonConfig.clientName();
        this.credentials = oidcClientCommonConfig.credentials();
    }

    /**
     * @param tokenPath {@link OidcClientCommonConfig#tokenPath()}
     * @return T builder
     */
    public T tokenPath(String tokenPath) {
        this.tokenPath = Optional.ofNullable(tokenPath);
        return getBuilder();
    }

    /**
     * @param revokePath {@link OidcClientCommonConfig#revokePath()}
     * @return T builder
     */
    public T revokePath(String revokePath) {
        this.revokePath = Optional.ofNullable(revokePath);
        return getBuilder();
    }

    /**
     * @param clientId {@link OidcClientCommonConfig#clientId()}
     * @return T builder
     */
    public T clientId(String clientId) {
        this.clientId = Optional.ofNullable(clientId);
        return getBuilder();
    }

    /**
     * @param clientName {@link OidcClientCommonConfig#clientName()}
     * @return T builder
     */
    public T clientName(String clientName) {
        this.clientName = Optional.ofNullable(clientName);
        return getBuilder();
    }

    /**
     * @param credentials {@link OidcClientCommonConfig#credentials()} created with {@link CredentialsBuilder} or SmallRye
     *        Config
     * @return T builder
     */
    public T credentials(Credentials credentials) {
        this.credentials = Objects.requireNonNull(credentials);
        return getBuilder();
    }

    /**
     * Creates {@link OidcClientCommonConfig#credentials()} builder.
     *
     * @return CredentialsBuilder
     */
    public CredentialsBuilder<T> credentials() {
        return new CredentialsBuilder<>(this);
    }

    /**
     * @param secret {@link Credentials#secret()}
     * @return T builder
     */
    public T credentials(String secret) {
        return credentials().secret(secret).end();
    }

    /**
     * @param clientSecret {@link Credentials#clientSecret()} created with {@link SecretBuilder} or SmallRye Config
     * @return T builder
     */
    public T credentials(Secret clientSecret) {
        Objects.requireNonNull(clientSecret);
        return credentials().clientSecret(clientSecret).end();
    }

    /**
     * @param jwt {@link Credentials#jwt()} created with {@link JwtBuilder} or SmallRye Config
     * @return T builder
     */
    public T credentials(Jwt jwt) {
        Objects.requireNonNull(jwt);
        return credentials().jwt(jwt).end();
    }

    /**
     * Builder for the {@link Credentials} config.
     */
    public static final class CredentialsBuilder<T> {

        private record CredentialsImpl(Optional<String> secret, Secret clientSecret, Jwt jwt) implements Credentials {
        }

        private final OidcClientCommonConfigBuilder<T> builder;
        private Optional<String> secret;
        private Secret clientSecret;
        private Jwt jwt;

        public CredentialsBuilder() {
            this(getConfigBuilderWithDefaults());
        }

        public CredentialsBuilder(OidcClientCommonConfigBuilder<T> builder) {
            this.builder = builder;
            this.secret = builder.credentials.secret();
            this.clientSecret = builder.credentials.clientSecret();
            this.jwt = builder.credentials.jwt();
        }

        /**
         * @param secret {@link Credentials#secret()}
         * @return this builder
         */
        public CredentialsBuilder<T> secret(String secret) {
            this.secret = Optional.ofNullable(secret);
            return this;
        }

        /**
         * @param clientSecret {@link Credentials#clientSecret()} created with the {@link SecretBuilder} or SmallRye Config
         * @return this builder
         */
        public CredentialsBuilder<T> clientSecret(Secret clientSecret) {
            this.clientSecret = Objects.requireNonNull(clientSecret);
            return this;
        }

        /**
         * Creates builder for the {@link Credentials#clientSecret()}.
         *
         * @return SecretBuilder
         */
        public SecretBuilder<T> clientSecret() {
            return new SecretBuilder<>(this);
        }

        /**
         * @param value {@link Secret#value()}
         * @return this builder
         */
        public CredentialsBuilder<T> clientSecret(String value) {
            return clientSecret().value(value).end();
        }

        /**
         * @param provider {@link Secret#provider()}
         * @return this builder
         */
        public CredentialsBuilder<T> clientSecret(Provider provider) {
            return clientSecret().provider(provider).end();
        }

        /**
         * @param value {@link Secret#value()}
         * @param method {@link Secret#method()}
         * @return this builder
         */
        public CredentialsBuilder<T> clientSecret(String value, Method method) {
            return clientSecret().value(value).method(method).end();
        }

        /**
         * @param jwt {@link Credentials#jwt()} created with the {@link JwtBuilder} or SmallRye Config
         * @return this builder
         */
        public CredentialsBuilder<T> jwt(Jwt jwt) {
            this.jwt = Objects.requireNonNull(jwt);
            return this;
        }

        /**
         * Creates builder for the {@link Credentials#jwt()}.
         *
         * @return JwtBuilder
         */
        public JwtBuilder<T> jwt() {
            return new JwtBuilder<>(this);
        }

        /**
         * Builds {@link Credentials} and returns the builder.
         *
         * @return T builder
         */
        public T end() {
            Objects.requireNonNull(builder);
            return builder.credentials(build());
        }

        /**
         * @return Credentials
         */
        public Credentials build() {
            return new CredentialsImpl(secret, clientSecret, jwt);
        }

        private static <T> OidcClientCommonConfigBuilder<T> getConfigBuilderWithDefaults() {
            final OidcClientCommonConfig clientCommonConfig = new SmallRyeConfigBuilder()
                    .addDiscoveredConverters()
                    .withMapping(OidcClientCommonConfig.class)
                    .build()
                    .getConfigMapping(OidcClientCommonConfig.class);
            return new OidcClientCommonConfigBuilder<>(clientCommonConfig) {
                @Override
                protected T getBuilder() {
                    throw new UnsupportedOperationException(
                            "Use the 'OidcClientCommonConfigBuilder.CredentialsBuilder#build' method instead");
                }
            };
        }
    }

    /**
     * The {@link Secret} builder.
     */
    public static final class SecretBuilder<T> {

        private record SecretImpl(Optional<String> value, Optional<Method> method, Provider provider) implements Secret {
        }

        private final CredentialsBuilder<T> builder;

        private Optional<String> value;
        private Optional<Method> method;
        private Provider provider;

        public SecretBuilder() {
            this.builder = null;
            this.value = Optional.empty();
            this.method = Optional.empty();
            this.provider = new ProviderBuilder<>().build();
        }

        public SecretBuilder(CredentialsBuilder<T> builder) {
            this.builder = Objects.requireNonNull(builder);
            this.value = builder.clientSecret.value();
            this.method = builder.clientSecret.method();
            this.provider = builder.clientSecret.provider();
        }

        /**
         * @param method {@link Secret#method()}
         * @return this builder
         */
        public SecretBuilder<T> method(Method method) {
            this.method = Optional.ofNullable(method);
            return this;
        }

        /**
         * @param value {@link Secret#value()}
         * @return this builder
         */
        public SecretBuilder<T> value(String value) {
            this.value = Optional.ofNullable(value);
            return this;
        }

        /**
         * @param provider {@link Secret#provider()} created with the {@link ProviderBuilder} or SmallRye Config
         * @return this builder
         */
        public SecretBuilder<T> provider(Provider provider) {
            this.provider = Objects.requireNonNull(provider);
            return this;
        }

        /**
         * Adds {@link Secret#provider()}.
         *
         * @param key {@link Provider#key()}
         * @return this builder
         */
        public SecretBuilder<T> provider(String key) {
            return provider().key(key).end();
        }

        /**
         * Adds {@link Secret#provider()}.
         *
         * @param key {@link Provider#key()}
         * @param name {@link Provider#name()}
         * @return this builder
         */
        public SecretBuilder<T> provider(String key, String name) {
            return provider().key(key).name(name).end();
        }

        /**
         * Adds {@link Secret#provider()}.
         *
         * @param key {@link Provider#key()}
         * @param name {@link Provider#name()}
         * @param keyringName {@link Provider#keyringName()}
         * @return this builder
         */
        public SecretBuilder<T> provider(String key, String name, String keyringName) {
            return provider().key(key).name(name).keyringName(keyringName).end();
        }

        /**
         * Creates {@link Secret#provider()} builder.
         *
         * @return ProviderBuilder
         */
        public ProviderBuilder<SecretBuilder<T>> provider() {
            return new ProviderBuilder<>(this::provider, provider);
        }

        /**
         * Builds {@link Secret} client secret.
         *
         * @return CredentialsBuilder
         */
        public CredentialsBuilder<T> end() {
            Objects.requireNonNull(builder);
            return builder.clientSecret(build());
        }

        /**
         * Builds {@link Credentials#clientSecret()} and {@link OidcClientCommonConfig#credentials()}.
         *
         * @return T builder
         */
        public T endCredentials() {
            return end().end();
        }

        public Secret build() {
            return new SecretImpl(value, method, provider);
        }
    }

    /**
     * The {@link Provider} builder.
     */
    public static final class ProviderBuilder<T> {

        private record ProviderImpl(Optional<String> name, Optional<String> keyringName,
                Optional<String> key) implements Provider {
        }

        private final Function<Provider, T> providerSetter;
        private Optional<String> name;
        private Optional<String> keyringName;
        private Optional<String> key;

        private ProviderBuilder(Function<Provider, T> providerSetter, Provider provider) {
            this.providerSetter = providerSetter;
            this.name = provider.name();
            this.keyringName = provider.keyringName();
            this.key = provider.key();
        }

        public ProviderBuilder() {
            this.providerSetter = null;
            this.name = Optional.empty();
            this.keyringName = Optional.empty();
            this.key = Optional.empty();
        }

        /**
         * @param name {@link Provider#name()}
         * @return this builder
         */
        public ProviderBuilder<T> name(String name) {
            this.name = Optional.ofNullable(name);
            return this;
        }

        /**
         * @param keyringName {@link Provider#keyringName()}
         * @return this builder
         */
        public ProviderBuilder<T> keyringName(String keyringName) {
            this.keyringName = Optional.ofNullable(keyringName);
            return this;
        }

        /**
         * @param key {@link Provider#key()}
         * @return this builder
         */
        public ProviderBuilder<T> key(String key) {
            this.key = Optional.ofNullable(key);
            return this;
        }

        /**
         * Builds {@link Provider}.
         *
         * @return T builder
         */
        public T end() {
            Objects.requireNonNull(providerSetter);
            return providerSetter.apply(build());
        }

        /**
         * Builds {@link Provider}.
         *
         * @return Provider
         */
        public Provider build() {
            return new ProviderImpl(name, keyringName, key);
        }

    }

    public static final class JwtBuilder<T> {

        private record JwtImpl(Source source, Optional<String> secret, Provider secretProvider, Optional<String> key,
                Optional<String> keyFile, Optional<String> keyStoreFile, Optional<String> keyStorePassword,
                Optional<String> keyId, Optional<String> keyPassword, Optional<String> audience, Optional<String> tokenKeyId,
                Optional<String> issuer, Optional<String> subject, Map<String, String> claims,
                Optional<String> signatureAlgorithm, int lifespan, boolean assertion,
                Optional<Path> tokenPath) implements Jwt {

        }

        private final CredentialsBuilder<T> builder;
        private final Map<String, String> claims = new HashMap<>();
        private Source source;
        private Optional<String> secret;
        private Provider secretProvider;
        private Optional<String> key;
        private Optional<String> keyFile;
        private Optional<String> keyStoreFile;
        private Optional<String> keyStorePassword;
        private Optional<String> keyId;
        private Optional<String> keyPassword;
        private Optional<String> audience;
        private Optional<String> tokenKeyId;
        private Optional<String> issuer;
        private Optional<String> subject;
        private Optional<String> signatureAlgorithm;
        private Optional<Path> tokenPath;
        private int lifespan;
        private boolean assertion;

        public JwtBuilder() {
            this.builder = null;
            this.source = Source.CLIENT;
            this.secret = Optional.empty();
            this.secretProvider = new ProviderBuilder<>().build();
            this.key = Optional.empty();
            this.keyFile = Optional.empty();
            this.keyStoreFile = Optional.empty();
            this.keyStorePassword = Optional.empty();
            this.keyId = Optional.empty();
            this.keyPassword = Optional.empty();
            this.audience = Optional.empty();
            this.tokenKeyId = Optional.empty();
            this.issuer = Optional.empty();
            this.subject = Optional.empty();
            this.signatureAlgorithm = Optional.empty();
            this.lifespan = 10;
            this.assertion = false;
            this.tokenPath = Optional.empty();
        }

        public JwtBuilder(CredentialsBuilder<T> builder) {
            this(Objects.requireNonNull(builder), builder.jwt);
        }

        private JwtBuilder(CredentialsBuilder<T> builder, Jwt jwt) {
            this.builder = builder;
            this.source = jwt.source();
            this.secret = jwt.secret();
            this.secretProvider = jwt.secretProvider();
            this.key = jwt.key();
            this.keyFile = jwt.keyFile();
            this.keyStoreFile = jwt.keyStoreFile();
            this.keyStorePassword = jwt.keyStorePassword();
            this.keyId = jwt.keyId();
            this.keyPassword = jwt.keyPassword();
            this.audience = jwt.audience();
            this.tokenKeyId = jwt.tokenKeyId();
            this.issuer = jwt.issuer();
            this.subject = jwt.subject();
            this.claims.putAll(jwt.claims());
            this.signatureAlgorithm = jwt.signatureAlgorithm();
            this.lifespan = jwt.lifespan();
            this.assertion = jwt.assertion();
            this.tokenPath = jwt.tokenPath();
        }

        /**
         * @param tokenPath {@link Jwt#tokenPath()}
         * @return this builder
         */
        public JwtBuilder<T> tokenPath(Path tokenPath) {
            this.tokenPath = Optional.ofNullable(tokenPath);
            return this;
        }

        /**
         * @return {@link Jwt#secretProvider()} builder
         */
        public ProviderBuilder<JwtBuilder<T>> secretProvider() {
            return new ProviderBuilder<>(this::secretProvider, secretProvider);
        }

        /**
         * @param secretProvider {@link Jwt#secretProvider()} created by {@link ProviderBuilder} or SmallRye Config
         * @return this builder
         */
        public JwtBuilder<T> secretProvider(Provider secretProvider) {
            Objects.requireNonNull(secretProvider);
            this.secretProvider = secretProvider;
            return this;
        }

        /**
         * @param source {@link Jwt#source()}
         * @return this builder
         */
        public JwtBuilder<T> source(Source source) {
            Objects.requireNonNull(source);
            this.source = source;
            return this;
        }

        /**
         * @param secret {@link Jwt#secret()}
         * @return this builder
         */
        public JwtBuilder<T> secret(String secret) {
            this.secret = Optional.ofNullable(secret);
            return this;
        }

        /**
         * @param key {@link Jwt#key()}
         * @return this builder
         */
        public JwtBuilder<T> key(String key) {
            this.key = Optional.ofNullable(key);
            return this;
        }

        /**
         * @param keyFile {@link Jwt#keyFile()}
         * @return this builder
         */
        public JwtBuilder<T> keyFile(String keyFile) {
            this.keyFile = Optional.ofNullable(keyFile);
            return this;
        }

        /**
         * @param keyStoreFile {@link Jwt#keyStoreFile()}
         * @return this builder
         */
        public JwtBuilder<T> keyStoreFile(String keyStoreFile) {
            this.keyStoreFile = Optional.ofNullable(keyStoreFile);
            return this;
        }

        /**
         * @param keyStorePassword {@link Jwt#keyStorePassword()}
         * @return this builder
         */
        public JwtBuilder<T> keyStorePassword(String keyStorePassword) {
            this.keyStorePassword = Optional.ofNullable(keyStorePassword);
            return this;
        }

        /**
         * @param keyId {@link Jwt#keyId()}
         * @return this builder
         */
        public JwtBuilder<T> keyId(String keyId) {
            this.keyId = Optional.ofNullable(keyId);
            return this;
        }

        /**
         * @param keyPassword {@link Jwt#keyPassword()}
         * @return this builder
         */
        public JwtBuilder<T> keyPassword(String keyPassword) {
            this.keyPassword = Optional.ofNullable(keyPassword);
            return this;
        }

        /**
         * @param audience {@link Jwt#audience()}
         * @return this builder
         */
        public JwtBuilder<T> audience(String audience) {
            this.audience = Optional.ofNullable(audience);
            return this;
        }

        /**
         * @param tokenKeyId {@link Jwt#tokenKeyId()}
         * @return this builder
         */
        public JwtBuilder<T> tokenKeyId(String tokenKeyId) {
            this.tokenKeyId = Optional.ofNullable(tokenKeyId);
            return this;
        }

        /**
         * @param issuer {@link Jwt#issuer()}
         * @return this builder
         */
        public JwtBuilder<T> issuer(String issuer) {
            this.issuer = Optional.ofNullable(issuer);
            return this;
        }

        /**
         * @param subject {@link Jwt#subject()}
         * @return this builder
         */
        public JwtBuilder<T> subject(String subject) {
            this.subject = Optional.ofNullable(subject);
            return this;
        }

        /**
         * @param claimName {@link Jwt#claims()} map entry key
         * @param claimValue {@link Jwt#claims()} map entry value
         * @return this builder
         */
        public JwtBuilder<T> claim(String claimName, String claimValue) {
            Objects.requireNonNull(claimName);
            Objects.requireNonNull(claimValue);
            this.claims.put(claimName, claimValue);
            return this;
        }

        /**
         * @param claims {@link Jwt#claims()}
         * @return this builder
         */
        public JwtBuilder<T> claims(Map<String, String> claims) {
            Objects.requireNonNull(claims);
            this.claims.putAll(claims);
            return this;
        }

        /**
         * @param signatureAlgorithm {@link Jwt#signatureAlgorithm()}
         * @return this builder
         */
        public JwtBuilder<T> signatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = Optional.ofNullable(signatureAlgorithm);
            return this;
        }

        /**
         * @param lifespan {@link Jwt#lifespan()}
         * @return this builder
         */
        public JwtBuilder<T> lifespan(int lifespan) {
            this.lifespan = lifespan;
            return this;
        }

        /**
         * @param assertion {@link Jwt#assertion()}
         * @return this builder
         */
        public JwtBuilder<T> assertion(boolean assertion) {
            this.assertion = assertion;
            return this;
        }

        /**
         * Builds {@link Credentials#jwt()}.
         *
         * @return CredentialsBuilder
         */
        public CredentialsBuilder<T> end() {
            Objects.requireNonNull(builder);
            return builder.jwt(build());
        }

        /**
         * Builds {@link Credentials#jwt()} and {@link OidcClientCommonConfig#credentials()}.
         *
         * @return T builder
         */
        public T endCredentials() {
            return end().end();
        }

        /**
         * Builds {@link Jwt}.
         *
         * @return Jwt
         */
        public Jwt build() {
            return new JwtImpl(source, secret, secretProvider, key, keyFile, keyStoreFile, keyStorePassword, keyId, keyPassword,
                    audience, tokenKeyId, issuer, subject, Map.copyOf(claims), signatureAlgorithm, lifespan, assertion,
                    tokenPath);
        }
    }
}
