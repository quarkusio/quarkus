package io.quarkus.vertx.http.security;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
     * Create a new CORS configuration.
     * This method is a shortcut for {@code builder().build()}.
     *
     * @return CORS instance, which should be passed to the {@link HttpSecurity} event
     */
    static CORS create() {
        return builder().build();
    }

    /**
     * Creates the CORS configuration builder.
     *
     * @return new {@link Builder} instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new CORS configuration with given origins.
     *
     * @param origins see {@link Builder#origins(List)}
     * @return CORS instance, which should be passed to the {@link HttpSecurity} event
     */
    static CORS origins(String... origins) {
        Objects.requireNonNull(origins, "Origins must not be null");
        return origins(List.of(origins));
    }

    /**
     * Create a new CORS configuration with given origins.
     *
     * @param origins see {@link Builder#origins(List)}
     * @return CORS instance, which should be passed to the {@link HttpSecurity} event
     */
    static CORS origins(List<String> origins) {
        return builder().origins(origins).build();
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
            this(HttpSecurityUtils.getDefaultVertxHttpConfig().cors());
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
         * This method is a shortcut for {@code exposedHeaders(List.of(exposedHeader))}.
         *
         * @return this builder
         */
        public Builder exposedHeader(String exposedHeader) {
            if (exposedHeader == null) {
                throw new IllegalArgumentException("Argument 'exposedHeader' cannot be null");
            }
            return exposedHeaders(List.of(exposedHeader));
        }

        /**
         * This method is a shortcut for {@code exposedHeaders(List.of(exposedHeaders))}.
         *
         * @return this builder
         */
        public Builder exposedHeaders(String... exposedHeaders) {
            if (exposedHeaders == null || exposedHeaders.length == 0) {
                throw new IllegalArgumentException("No exposed headers specified");
            }
            return exposedHeaders(List.of(exposedHeaders));
        }

        /**
         * @param exposedHeaders {@link CORSConfig#exposedHeaders()}
         * @return this builder
         */
        public Builder exposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = merge(this.exposedHeaders, exposedHeaders, "Exposed headers");
            return this;
        }

        /**
         * This method is a shortcut for {@code headers(List.of(header))}.
         *
         * @return this builder
         */
        public Builder header(String header) {
            if (header == null) {
                throw new IllegalArgumentException("Argument 'header' cannot be null");
            }
            return headers(List.of(header));
        }

        /**
         * This method is a shortcut for {@code headers(List.of(headers))}.
         *
         * @return this builder
         */
        public Builder headers(String... headers) {
            if (headers == null || headers.length == 0) {
                throw new IllegalArgumentException("No headers specified");
            }
            return headers(List.of(headers));
        }

        /**
         * @param newHeaders {@link CORSConfig#headers()}
         * @return this builder
         */
        public Builder headers(List<String> newHeaders) {
            this.headers = merge(this.headers, newHeaders, "Headers");
            return this;
        }

        /**
         * This method is a shortcut for {@code methods(List.of(method))}.
         *
         * @return this builder
         */
        public Builder method(String method) {
            if (method == null) {
                throw new IllegalArgumentException("Argument 'method' cannot be null");
            }
            return methods(List.of(method));
        }

        /**
         * This method is a shortcut for {@code methods(List.of(methods))}.
         *
         * @return this builder
         */
        public Builder methods(String... methods) {
            if (methods == null || methods.length == 0) {
                throw new IllegalArgumentException("No methods specified");
            }
            return methods(List.of(methods));
        }

        /**
         * @param newMethods {@link CORSConfig#methods()}
         * @return this builder
         */
        public Builder methods(List<String> newMethods) {
            this.methods = merge(this.methods, newMethods, "Methods");
            return this;
        }

        /**
         * This method is a shortcut for {@code origins(List.of(origin))}.
         *
         * @return this builder
         */
        public Builder origin(String origin) {
            if (origin == null) {
                throw new IllegalArgumentException("Argument 'origin' cannot be null");
            }
            return origins(List.of(origin));
        }

        /**
         * This method is a shortcut for {@code origins(List.of(origins))}.
         *
         * @return this builder
         */
        public Builder origins(String... origins) {
            if (origins == null || origins.length == 0) {
                throw new IllegalArgumentException("No origins specified");
            }
            return origins(List.of(origins));
        }

        /**
         * @param newOrigins {@link CORSConfig#origins()}
         * @return this builder
         */
        public Builder origins(List<String> newOrigins) {
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

        private static Optional<List<String>> merge(Optional<List<String>> optionalOriginalList, List<String> newList,
                String what) {
            if (newList == null) {
                throw new IllegalArgumentException(what + " must not be null");
            }
            if (newList.isEmpty()) {
                return optionalOriginalList;
            }
            final List<String> result;
            if (optionalOriginalList.orElse(List.of()).isEmpty()) {
                result = List.copyOf(newList);
            } else {
                result = Stream.concat(optionalOriginalList.get().stream(), newList.stream()).toList();
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
