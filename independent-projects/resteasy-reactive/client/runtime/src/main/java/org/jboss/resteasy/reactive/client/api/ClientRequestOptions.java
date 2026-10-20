package org.jboss.resteasy.reactive.client.api;

import java.util.concurrent.TimeUnit;

/**
 * Options of a single REST Client request.
 * <p>
 * When a REST Client method has a parameter of this type, the options passed to it apply to that invocation only:
 * the options that are set override the configuration of the client, the others keep it. The parameter is not sent
 * with the request, and passing {@code null} keeps the configuration of the client.
 *
 * <pre>
 * &#64;GET
 * &#64;Path("/reports/{id}")
 * Report get(@PathParam("id") long id, ClientRequestOptions options);
 * </pre>
 *
 * The options can also be set from a {@code ClientRequestFilter}, as the
 * {@link QuarkusRestClientProperties#REQUEST_OPTIONS} request property.
 */
public final class ClientRequestOptions {

    private final Long connectTimeout;
    private final Long readTimeout;
    private final Boolean followRedirects;

    private ClientRequestOptions(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.followRedirects = builder.followRedirects;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the connect timeout in milliseconds, or {@code null} to use the one of the client
     */
    public Long getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * @return the read timeout in milliseconds, or {@code null} to use the one of the client
     */
    public Long getReadTimeout() {
        return readTimeout;
    }

    /**
     * @return whether redirects are followed, or {@code null} to use the setting of the client
     */
    public Boolean getFollowRedirects() {
        return followRedirects;
    }

    public static final class Builder {

        private Long connectTimeout;
        private Long readTimeout;
        private Boolean followRedirects;

        private Builder() {
        }

        /**
         * Sets the connect timeout for the request.
         *
         * @throws IllegalArgumentException if the timeout is negative
         */
        public Builder connectTimeout(long timeout, TimeUnit unit) {
            this.connectTimeout = toMillis(timeout, unit);
            return this;
        }

        /**
         * Sets the read timeout for the request.
         *
         * @throws IllegalArgumentException if the timeout is negative
         */
        public Builder readTimeout(long timeout, TimeUnit unit) {
            this.readTimeout = toMillis(timeout, unit);
            return this;
        }

        /**
         * Sets whether the request follows redirects.
         */
        public Builder followRedirects(boolean follow) {
            this.followRedirects = follow;
            return this;
        }

        public ClientRequestOptions build() {
            return new ClientRequestOptions(this);
        }

        private static long toMillis(long timeout, TimeUnit unit) {
            if (timeout < 0) {
                throw new IllegalArgumentException("Timeout must not be negative: " + timeout);
            }
            return unit.toMillis(timeout);
        }
    }
}
