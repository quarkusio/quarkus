package io.quarkus.vertx.http.security;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.common.annotation.Experimental;

/**
 * This class provides a way to configure the Quarkus cross-origin resource sharing (CORS) filter at the HTTP layer level.
 */
@Experimental("This API is currently experimental and might get changed")
public sealed interface CORS permits CORS.Builder.CORSImpl {

    /**
     * Creates the CORS configuration builder.
     *
     * @return new {@link Builder} instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new CORS configuration builder with given origins.
     *
     * @param origins see {@link Builder#origins(Set)}
     * @return new {@link Builder} instance with given origins
     */
    static Builder origins(Set<String> origins) {
        return builder().origins(origins);
    }

    /**
     * The Quarkus CORS filter configuration builder.
     */
    final class Builder {

        private Optional<Boolean> accessControlAllowCredentials;
        private Optional<Duration> accessControlMaxAge;
        private Optional<List<String>> exposedHeaders;
        private Optional<List<String>> headers;
        private Optional<List<String>> methods;
        private Optional<List<String>> origins;

        public Builder() {
            this(HttpSecurityUtils.getDefaultAuthConfig().cors());
        }

        public Builder(CORSConfig corsConfig) {
            this.accessControlAllowCredentials = corsConfig.accessControlAllowCredentials();
            this.accessControlMaxAge = corsConfig.accessControlMaxAge();
            this.exposedHeaders = corsConfig.exposedHeaders();
            this.headers = corsConfig.headers();
            this.methods = corsConfig.methods();
            this.origins = corsConfig.origins();
        }

        /**
         * @param accessControlMaxAge {@link CORSConfig#accessControlMaxAge()}
         * @return this builder
         */
        public Builder accessControlMaxAge(Duration accessControlMaxAge) {
            Objects.requireNonNull(accessControlMaxAge, "accessControlMaxAge argument must not be null");
            this.accessControlMaxAge = Optional.of(accessControlMaxAge);
            return this;
        }

        /**
         * This method is a shortcut for {@code accessControlAllowCredentials(true)}.
         *
         * @return this builder
         */
        public Builder accessControlAllowCredentials() {
            return accessControlAllowCredentials(true);
        }

        /**
         * @param accessControlAllowCredentials {@link CORSConfig#accessControlAllowCredentials()}
         * @return this builder
         */
        public Builder accessControlAllowCredentials(boolean accessControlAllowCredentials) {
            this.accessControlAllowCredentials = Optional.of(accessControlAllowCredentials);
            return this;
        }

        /**
         * This method is a shortcut for {@code exposedHeaders(Set.of(exposedHeader))}.
         *
         * @return this builder
         */
        public Builder exposedHeader(String exposedHeader) {
            if (exposedHeader == null) {
                throw new IllegalArgumentException("Argument 'exposedHeader' cannot be null");
            }
            return exposedHeaders(Set.of(exposedHeader));
        }

        /**
         * @param exposedHeaders {@link CORSConfig#exposedHeaders()}
         * @return this builder
         */
        public Builder exposedHeaders(Set<String> exposedHeaders) {
            this.exposedHeaders = merge(this.exposedHeaders, exposedHeaders, "Exposed headers");
            return this;
        }

        /**
         * This method is a shortcut for {@code headers(Set.of(header))}.
         *
         * @return this builder
         */
        public Builder header(String header) {
            if (header == null) {
                throw new IllegalArgumentException("Argument 'header' cannot be null");
            }
            return headers(Set.of(header));
        }

        /**
         * @param newHeaders {@link CORSConfig#headers()}
         * @return this builder
         */
        public Builder headers(Set<String> newHeaders) {
            this.headers = merge(this.headers, newHeaders, "Headers");
            return this;
        }

        /**
         * This method is a shortcut for {@code methods(Set.of(method))}.
         *
         * @return this builder
         */
        public Builder method(String method) {
            if (method == null) {
                throw new IllegalArgumentException("Argument 'method' cannot be null");
            }
            return methods(Set.of(method));
        }

        /**
         * @param newMethods {@link CORSConfig#methods()}
         * @return this builder
         */
        public Builder methods(Set<String> newMethods) {
            this.methods = merge(this.methods, newMethods, "Methods");
            return this;
        }

        /**
         * This method is a shortcut for {@code origins(Set.of(origin))}.
         *
         * @return this builder
         */
        public Builder origin(String origin) {
            if (origin == null) {
                throw new IllegalArgumentException("Argument 'origin' cannot be null");
            }
            return origins(Set.of(origin));
        }

        /**
         * @param newOrigins {@link CORSConfig#origins()}
         * @return this builder
         */
        public Builder origins(Set<String> newOrigins) {
            this.origins = merge(this.origins, newOrigins, "Origins");
            return this;
        }

        /**
         * Create a new CORS configuration.
         *
         * @return CORS instance, which should be passed to the {@link HttpSecurity} event
         */
        public CORS build() {
            return new CORSImpl(accessControlAllowCredentials, accessControlMaxAge, exposedHeaders, headers, methods, origins);
        }

        private static Optional<List<String>> merge(Optional<List<String>> optionalOriginalList, Set<String> newSet,
                String what) {
            if (newSet == null) {
                throw new IllegalArgumentException(what + " must not be null");
            }
            if (newSet.isEmpty()) {
                return optionalOriginalList;
            }
            final List<String> result;
            if (optionalOriginalList.orElse(List.of()).isEmpty()) {
                result = List.copyOf(newSet);
            } else {
                result = Stream.concat(optionalOriginalList.get().stream(), newSet.stream()).toList();
            }
            return Optional.of(result);
        }

        record CORSImpl(Optional<Boolean> accessControlAllowCredentials, Optional<Duration> accessControlMaxAge,
                Optional<List<String>> exposedHeaders, Optional<List<String>> headers,
                Optional<List<String>> methods, Optional<List<String>> origins) implements CORS, CORSConfig {
            @Override
            public boolean enabled() {
                return true;
            }
        }
    }
}
