package org.jboss.resteasy.reactive.client.api;

public class QuarkusRestClientProperties {
    public static final String CONNECT_TIMEOUT = "io.quarkus.rest.client.connect-timeout";
    /**
     * maximum number of redirects for a client call. Works only if the client has `followingRedirects enabled
     */
    public static final String MAX_REDIRECTS = "io.quarkus.rest.client.max-redirects";
    public static final String READ_TIMEOUT = "io.quarkus.rest.client.read-timeout";
}
