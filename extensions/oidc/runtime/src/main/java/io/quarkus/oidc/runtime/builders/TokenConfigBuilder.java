package io.quarkus.oidc.runtime.builders;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.quarkus.oidc.OidcTenantConfigBuilder;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig.Binding;

/**
 * Builder for the {@link OidcTenantConfig.Token}.
 */
public final class TokenConfigBuilder {

    private record TokenImpl(Optional<String> issuer, Optional<List<String>> audience, boolean subjectRequired,
            Map<String, Set<String>> requiredClaims, Optional<String> tokenType, OptionalInt lifespanGrace,
            Optional<Duration> age, boolean issuedAtRequired, Optional<String> principalClaim, boolean refreshExpired,
            Optional<Duration> refreshTokenTimeSkew, Duration forcedJwkRefreshInterval, Optional<String> header,
            String authorizationScheme, Optional<OidcTenantConfig.SignatureAlgorithm> signatureAlgorithm,
            Optional<String> decryptionKeyLocation, Optional<Boolean> decryptIdToken, boolean decryptAccessToken,
            boolean allowJwtIntrospection, boolean requireJwtIntrospectionOnly,
            boolean allowOpaqueTokenIntrospection, Optional<String> customizerName,
            Optional<Boolean> verifyAccessTokenWithUserInfo, Binding binding) implements OidcTenantConfig.Token {
    }

    private final OidcTenantConfigBuilder builder;
    private final Map<String, Set<String>> requiredClaims = new HashMap<>();
    private final List<String> audience = new ArrayList<>();
    private Optional<String> issuer;
    private boolean subjectRequired;
    private Optional<String> tokenType;
    private OptionalInt lifespanGrace;
    private Optional<Duration> age;
    private boolean issuedAtRequired;
    private Optional<String> principalClaim;
    private boolean refreshExpired;
    private Optional<Duration> refreshTokenTimeSkew;
    private Duration forcedJwkRefreshInterval;
    private Optional<String> header;
    private String authorizationScheme;
    private Optional<OidcTenantConfig.SignatureAlgorithm> signatureAlgorithm;
    private Optional<String> decryptionKeyLocation;
    Optional<Boolean> decryptIdToken;
    private boolean decryptAccessToken;
    private boolean allowJwtIntrospection;
    private boolean requireJwtIntrospectionOnly;
    private boolean allowOpaqueTokenIntrospection;
    private Optional<String> customizerName;
    private Optional<Boolean> verifyAccessTokenWithUserInfo;
    private Binding binding;

    public TokenConfigBuilder() {
        this(new OidcTenantConfigBuilder());
    }

    public TokenConfigBuilder(OidcTenantConfigBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
        var token = builder.getToken();
        if (!token.requiredClaims().isEmpty()) {
            this.requiredClaims.putAll(token.requiredClaims());
        }
        if (token.audience().isPresent()) {
            this.audience.addAll(token.audience().get());
        }
        this.issuer = token.issuer();
        this.subjectRequired = token.subjectRequired();
        this.tokenType = token.tokenType();
        this.lifespanGrace = token.lifespanGrace();
        this.age = token.age();
        this.issuedAtRequired = token.issuedAtRequired();
        this.principalClaim = token.principalClaim();
        this.refreshExpired = token.refreshExpired();
        this.refreshTokenTimeSkew = token.refreshTokenTimeSkew();
        this.forcedJwkRefreshInterval = token.forcedJwkRefreshInterval();
        this.header = token.header();
        this.authorizationScheme = token.authorizationScheme();
        this.signatureAlgorithm = token.signatureAlgorithm();
        this.decryptionKeyLocation = token.decryptionKeyLocation();
        this.decryptIdToken = token.decryptIdToken();
        this.decryptAccessToken = token.decryptAccessToken();
        this.allowJwtIntrospection = token.allowJwtIntrospection();
        this.requireJwtIntrospectionOnly = token.requireJwtIntrospectionOnly();
        this.allowOpaqueTokenIntrospection = token.allowOpaqueTokenIntrospection();
        this.customizerName = token.customizerName();
        this.verifyAccessTokenWithUserInfo = token.verifyAccessTokenWithUserInfo();
        this.binding = token.binding();
    }

    /**
     * @return OidcTenantConfigBuilder builder
     */
    public OidcTenantConfigBuilder end() {
        return builder.token(build());
    }

    /**
     * @param requiredClaimName {@link OidcTenantConfig.Token#requiredClaims()} name
     * @param requiredClaimValue {@link OidcTenantConfig.Token#requiredClaims()} value
     * @return this builder
     */
    public TokenConfigBuilder requiredClaims(String requiredClaimName, String requiredClaimValue) {
        Objects.requireNonNull(requiredClaimName);
        Objects.requireNonNull(requiredClaimValue);
        this.requiredClaims.put(requiredClaimName, Set.of(requiredClaimValue));
        return this;
    }

    /**
     * @param requiredClaimName {@link OidcTenantConfig.Token#requiredClaims()} name
     * @param requiredClaimValues {@link OidcTenantConfig.Token#requiredClaims()} value
     * @return this builder
     */
    public TokenConfigBuilder requiredClaims(String requiredClaimName, Set<String> requiredClaimValues) {
        Objects.requireNonNull(requiredClaimName);
        Objects.requireNonNull(requiredClaimValues);
        this.requiredClaims.put(requiredClaimName, Set.copyOf(requiredClaimValues));
        return this;
    }

    /**
     * @param requiredClaims {@link OidcTenantConfig.Token#requiredClaims()}
     * @return this builder
     */
    public TokenConfigBuilder requiredClaims(Map<String, String> requiredClaims) {
        if (requiredClaims != null) {
            return this.setRequiredClaims(requiredClaims
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(new Function<Map.Entry<String, String>, String>() {
                        @Override
                        public String apply(Map.Entry<String, String> stringStringEntry) {
                            return stringStringEntry.getKey();
                        }
                    }, new Function<Map.Entry<String, String>, Set<String>>() {
                        @Override
                        public Set<String> apply(Map.Entry<String, String> e) {
                            return Set.of(e.getValue());
                        }
                    })));
        }
        return this;
    }

    /**
     * @param requiredClaims {@link OidcTenantConfig.Token#requiredClaims()}
     * @return this builder
     */
    public TokenConfigBuilder setRequiredClaims(Map<String, Set<String>> requiredClaims) {
        if (requiredClaims != null) {
            this.requiredClaims.putAll(requiredClaims);
        }
        return this;
    }

    /**
     * @param audience {@link OidcTenantConfig.Token#audience()}
     * @return this builder
     */
    public TokenConfigBuilder audience(String... audience) {
        if (audience != null) {
            this.audience.addAll(Arrays.asList(audience));
        }
        return this;
    }

    /**
     * @param audience {@link OidcTenantConfig.Token#audience()}
     * @return this builder
     */
    public TokenConfigBuilder audience(List<String> audience) {
        if (audience != null) {
            this.audience.addAll(audience);
        }
        return this;
    }

    /**
     * @param issuer {@link OidcTenantConfig.Token#issuer()}
     * @return this builder
     */
    public TokenConfigBuilder issuer(String issuer) {
        this.issuer = Optional.ofNullable(issuer);
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig.Token#subjectRequired()} to true.
     *
     * @return this builder
     */
    public TokenConfigBuilder subjectRequired() {
        return subjectRequired(true);
    }

    /**
     * @param subjectRequired {@link OidcTenantConfig.Token#subjectRequired()}
     * @return this builder
     */
    public TokenConfigBuilder subjectRequired(boolean subjectRequired) {
        this.subjectRequired = subjectRequired;
        return this;
    }

    /**
     * @param tokenType {@link OidcTenantConfig.Token#tokenType()}
     * @return this builder
     */
    public TokenConfigBuilder tokenType(String tokenType) {
        this.tokenType = Optional.ofNullable(tokenType);
        return this;
    }

    /**
     * @param lifespanGrace {@link OidcTenantConfig.Token#lifespanGrace()}
     * @return this builder
     */
    public TokenConfigBuilder lifespanGrace(int lifespanGrace) {
        this.lifespanGrace = OptionalInt.of(lifespanGrace);
        return this;
    }

    /**
     * @param age {@link OidcTenantConfig.Token#age()}
     * @return this builder
     */
    public TokenConfigBuilder age(Duration age) {
        this.age = Optional.ofNullable(age);
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig.Token#issuedAtRequired()} to true.
     *
     * @return this builder
     */
    public TokenConfigBuilder issuedAtRequired() {
        return issuedAtRequired(true);
    }

    /**
     * @param issuedAtRequired {@link OidcTenantConfig.Token#issuedAtRequired()}
     * @return this builder
     */
    public TokenConfigBuilder issuedAtRequired(boolean issuedAtRequired) {
        this.issuedAtRequired = issuedAtRequired;
        return this;
    }

    /**
     * @param principalClaim {@link OidcTenantConfig.Token#principalClaim()}
     * @return this builder
     */
    public TokenConfigBuilder principalClaim(String principalClaim) {
        this.principalClaim = Optional.ofNullable(principalClaim);
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig.Token#refreshExpired()} to true.
     *
     * @return this builder
     */
    public TokenConfigBuilder refreshExpired() {
        return refreshExpired(true);
    }

    /**
     * @param refreshExpired {@link OidcTenantConfig.Token#refreshExpired()}
     * @return this builder
     */
    public TokenConfigBuilder refreshExpired(boolean refreshExpired) {
        this.refreshExpired = refreshExpired;
        return this;
    }

    /**
     * @param refreshTokenTimeSkew {@link OidcTenantConfig.Token#refreshTokenTimeSkew()}
     * @return this builder
     */
    public TokenConfigBuilder refreshTokenTimeSkew(Duration refreshTokenTimeSkew) {
        this.refreshTokenTimeSkew = Optional.ofNullable(refreshTokenTimeSkew);
        return this;
    }

    /**
     * @param forcedJwkRefreshInterval {@link OidcTenantConfig.Token#forcedJwkRefreshInterval()}
     * @return this builder
     */
    public TokenConfigBuilder forcedJwkRefreshInterval(Duration forcedJwkRefreshInterval) {
        this.forcedJwkRefreshInterval = Objects.requireNonNull(forcedJwkRefreshInterval);
        return this;
    }

    /**
     * @param header {@link OidcTenantConfig.Token#header()}
     * @return this builder
     */
    public TokenConfigBuilder header(String header) {
        this.header = Optional.ofNullable(header);
        return this;
    }

    /**
     * @param authorizationScheme {@link OidcTenantConfig.Token#authorizationScheme()}
     * @return this builder
     */
    public TokenConfigBuilder authorizationScheme(String authorizationScheme) {
        this.authorizationScheme = Objects.requireNonNull(authorizationScheme);
        return this;
    }

    /**
     * @param signatureAlgorithm {@link OidcTenantConfig.Token#signatureAlgorithm()}
     * @return this builder
     */
    public TokenConfigBuilder signatureAlgorithm(OidcTenantConfig.SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = Optional.ofNullable(signatureAlgorithm);
        return this;
    }

    /**
     * @param decryptionKeyLocation {@link OidcTenantConfig.Token#decryptionKeyLocation()}
     * @return this builder
     */
    public TokenConfigBuilder decryptionKeyLocation(String decryptionKeyLocation) {
        this.decryptionKeyLocation = Optional.ofNullable(decryptionKeyLocation);
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig.Token#decryptIdToken()} to true
     *
     * @return this builder
     */
    public TokenConfigBuilder decryptIdToken() {
        return decryptIdToken(true);
    }

    /**
     * @param decryptIdToken {@link OidcTenantConfig.Token#decryptIdToken()}
     * @return this builder
     */
    public TokenConfigBuilder decryptIdToken(boolean decryptIdToken) {
        this.decryptIdToken = Optional.of(decryptIdToken);
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig.Token#decryptAccessToken()} to true.
     *
     * @return this builder
     */
    public TokenConfigBuilder decryptAccessToken() {
        return decryptAccessToken(true);
    }

    /**
     * @param decryptAccessToken {@link OidcTenantConfig.Token#decryptAccessToken()}
     * @return this builder
     */
    public TokenConfigBuilder decryptAccessToken(boolean decryptAccessToken) {
        this.decryptAccessToken = decryptAccessToken;
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig.Token#allowJwtIntrospection()} to true.
     *
     * @return this builder
     */
    public TokenConfigBuilder allowJwtIntrospection() {
        return allowJwtIntrospection(true);
    }

    /**
     * @param allowJwtIntrospection {@link OidcTenantConfig.Token#allowJwtIntrospection()}
     * @return this builder
     */
    public TokenConfigBuilder allowJwtIntrospection(boolean allowJwtIntrospection) {
        this.allowJwtIntrospection = allowJwtIntrospection;
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig.Token#requireJwtIntrospectionOnly()} to true.
     *
     * @return this builder
     */
    public TokenConfigBuilder requireJwtIntrospectionOnly() {
        return requireJwtIntrospectionOnly(true);
    }

    /**
     * @param requireJwtIntrospectionOnly {@link OidcTenantConfig.Token#requireJwtIntrospectionOnly()}
     * @return this builder
     */
    public TokenConfigBuilder requireJwtIntrospectionOnly(boolean requireJwtIntrospectionOnly) {
        this.requireJwtIntrospectionOnly = requireJwtIntrospectionOnly;
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig.Token#allowOpaqueTokenIntrospection()} to true.
     *
     * @return this builder
     */
    public TokenConfigBuilder allowOpaqueTokenIntrospection() {
        return allowOpaqueTokenIntrospection(true);
    }

    /**
     * @param allowOpaqueTokenIntrospection {@link OidcTenantConfig.Token#allowOpaqueTokenIntrospection()}
     * @return this builder
     */
    public TokenConfigBuilder allowOpaqueTokenIntrospection(boolean allowOpaqueTokenIntrospection) {
        this.allowOpaqueTokenIntrospection = allowOpaqueTokenIntrospection;
        return this;
    }

    /**
     * @param customizerName {@link OidcTenantConfig.Token#customizerName()}
     * @return this builder
     */
    public TokenConfigBuilder customizerName(String customizerName) {
        this.customizerName = Optional.ofNullable(customizerName);
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig.Token#verifyAccessTokenWithUserInfo()} to true.
     *
     * @return this builder
     */
    public TokenConfigBuilder verifyAccessTokenWithUserInfo() {
        return verifyAccessTokenWithUserInfo(true);
    }

    /**
     * @param verifyAccessTokenWithUserInfo {@link OidcTenantConfig.Token#verifyAccessTokenWithUserInfo()}
     * @return this builder
     */
    public TokenConfigBuilder verifyAccessTokenWithUserInfo(boolean verifyAccessTokenWithUserInfo) {
        this.verifyAccessTokenWithUserInfo = Optional.of(verifyAccessTokenWithUserInfo);
        return this;
    }

    /**
     * binding {@link OidcTenantConfig.Token#binding()}
     *
     * @return BindingConfigBuilder
     */
    public BindingConfigBuilder binding() {
        return new BindingConfigBuilder(this);
    }

    /**
     * @param binding {@link OidcTenantConfig#)}
     * @return this builder
     */
    public TokenConfigBuilder binding(Binding binding) {
        this.binding = Objects.requireNonNull(binding);
        return this;
    }

    /**
     * @return current {@link Binding} instance
     */
    public Binding getBinding() {
        return binding;
    }

    /**
     * @return built {@link OidcTenantConfig.Token}
     */
    public OidcTenantConfig.Token build() {
        Optional<List<String>> optionalAudience = audience.isEmpty() ? Optional.empty()
                : Optional.of(List.copyOf(audience));
        return new TokenImpl(issuer, optionalAudience, subjectRequired, Map.copyOf(requiredClaims), tokenType,
                lifespanGrace, age, issuedAtRequired, principalClaim, refreshExpired, refreshTokenTimeSkew,
                forcedJwkRefreshInterval, header, authorizationScheme, signatureAlgorithm, decryptionKeyLocation,
                decryptIdToken,
                decryptAccessToken, allowJwtIntrospection, requireJwtIntrospectionOnly, allowOpaqueTokenIntrospection,
                customizerName,
                verifyAccessTokenWithUserInfo, binding);
    }

    /**
     * Builder for the {@link OidcTenantConfig.Token}.
     */
    public static final class BindingConfigBuilder {

        private record BindingImpl(boolean certificate) implements OidcTenantConfig.Binding {
        }

        private final TokenConfigBuilder builder;
        private boolean certificate;

        public BindingConfigBuilder() {
            this(new TokenConfigBuilder());
        }

        public BindingConfigBuilder(TokenConfigBuilder builder) {
            this.builder = Objects.requireNonNull(builder);
            var binding = builder.getBinding();
            this.certificate = binding.certificate();
        }

        /**
         * @return TokenConfigBuilder builder
         */
        public TokenConfigBuilder end() {
            return builder.binding(build());
        }

        /**
         * Sets {@link OidcTenantConfig.Binding#certificate()} to true.
         *
         * @return this builder
         */
        public BindingConfigBuilder certificate() {
            return certificate(true);
        }

        /**
         * @param certificate {@link OidcTenantConfig.Binding#certificate()}
         * @return this builder
         */
        public BindingConfigBuilder certificate(boolean certificate) {
            this.certificate = certificate;
            return this;
        }

        /**
         * @return built {@link OidcTenantConfig.Token}
         */
        public OidcTenantConfig.Binding build() {
            return new BindingImpl(certificate);
        }

    }

}
