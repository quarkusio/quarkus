package io.quarkus.csrf.reactive.runtime;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.vertx.http.security.CSRF;
import io.smallrye.config.SmallRyeConfig;

public final class RestCsrfBuilder implements CSRF.Builder {

    private String formFieldName;
    private String tokenHeaderName;
    private String cookieName;
    private Duration cookieMaxAge;
    private String cookiePath;
    private Optional<String> cookieDomain;
    private boolean cookieForceSecure;
    private boolean cookieHttpOnly;
    private Optional<Set<String>> createTokenPath;
    private int tokenSize;
    private Optional<String> tokenSignatureKey;
    private boolean verifyToken;
    private boolean requireFormUrlEncoded;

    // this method is used by a generated method, do not remove it
    public RestCsrfBuilder() {
        this(getRestCsrfConfig());
    }

    private RestCsrfBuilder(RestCsrfConfig config) {
        this.formFieldName = config.formFieldName();
        this.tokenHeaderName = config.tokenHeaderName();
        this.cookieName = config.cookieName();
        this.cookieMaxAge = config.cookieMaxAge();
        this.cookiePath = config.cookiePath();
        this.cookieDomain = config.cookieDomain();
        this.cookieForceSecure = config.cookieForceSecure();
        this.cookieHttpOnly = config.cookieHttpOnly();
        this.createTokenPath = config.createTokenPath();
        this.tokenSize = config.tokenSize();
        this.tokenSignatureKey = config.tokenSignatureKey();
        this.verifyToken = config.verifyToken();
        this.requireFormUrlEncoded = config.requireFormUrlEncoded();
    }

    @Override
    public CSRF.Builder formFieldName(String formFieldName) {
        this.formFieldName = requireNonNull(formFieldName, "Form field name");
        return this;
    }

    @Override
    public CSRF.Builder tokenHeaderName(String tokenHeaderName) {
        this.tokenHeaderName = requireNonNull(tokenHeaderName, "Token header name");
        return this;
    }

    @Override
    public CSRF.Builder cookieName(String cookieName) {
        this.cookieName = requireNonNull(cookieName, "Cookie name");
        return this;
    }

    @Override
    public CSRF.Builder cookieMaxAge(Duration cookieMaxAge) {
        this.cookieMaxAge = requireNonNull(cookieMaxAge, "Cookie max age");
        return this;
    }

    @Override
    public CSRF.Builder cookiePath(String cookiePath) {
        this.cookiePath = requireNonNull(cookiePath, "Cookie path");
        return this;
    }

    @Override
    public CSRF.Builder cookieDomain(String cookieDomain) {
        this.cookieDomain = Optional.of(requireNonNull(cookieDomain, "Cookie domain"));
        return this;
    }

    @Override
    public CSRF.Builder cookieForceSecure() {
        this.cookieForceSecure = true;
        return this;
    }

    @Override
    public CSRF.Builder cookieHttpOnly(boolean cookieHttpOnly) {
        this.cookieHttpOnly = cookieHttpOnly;
        return this;
    }

    @Override
    public CSRF.Builder createTokenPath(String createTokenPath) {
        requireNonNull(createTokenPath, "Create token path");
        return createTokenPath(Set.of(createTokenPath));
    }

    @Override
    public CSRF.Builder createTokenPath(Set<String> createTokenPath) {
        requireNonNull(createTokenPath, "Create token path");
        if (this.createTokenPath.isEmpty()) {
            this.createTokenPath = Optional.of(Set.copyOf(createTokenPath));
        } else {
            var newPaths = new HashSet<>(this.createTokenPath.get());
            newPaths.addAll(createTokenPath);
            this.createTokenPath = Optional.of(newPaths);
        }
        return this;
    }

    @Override
    public CSRF.Builder tokenSize(int tokenSize) {
        this.tokenSize = tokenSize;
        return this;
    }

    @Override
    public CSRF.Builder tokenSignatureKey(String tokenSignatureKey) {
        this.tokenSignatureKey = Optional.of(requireNonNull(tokenSignatureKey, "Token signature key"));
        return this;
    }

    @Override
    public CSRF.Builder requireFormUrlEncoded(boolean requireFormUrlEncoded) {
        this.requireFormUrlEncoded = requireFormUrlEncoded;
        return this;
    }

    @Override
    public CSRF build() {
        record CSRFImpl(String formFieldName, String tokenHeaderName, String cookieName, Duration cookieMaxAge,
                String cookiePath, Optional<String> cookieDomain, boolean cookieForceSecure,
                boolean cookieHttpOnly, Optional<Set<String>> createTokenPath, int tokenSize,
                Optional<String> tokenSignatureKey, boolean verifyToken,
                boolean requireFormUrlEncoded) implements RestCsrfConfig, CSRF {
        }
        return new CSRFImpl(formFieldName, tokenHeaderName, cookieName, cookieMaxAge, cookiePath, cookieDomain,
                cookieForceSecure, cookieHttpOnly, createTokenPath, tokenSize, tokenSignatureKey, verifyToken,
                requireFormUrlEncoded);
    }

    private static <T> T requireNonNull(T value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " must not be null");
        }
        return value;
    }

    private static RestCsrfConfig getRestCsrfConfig() {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getConfigMapping(RestCsrfConfig.class);
    }
}
