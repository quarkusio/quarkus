package org.jboss.resteasy.reactive.client.api;

public class QuarkusRestClientProperties {
    public static final String CONNECT_TIMEOUT = "io.quarkus.rest.client.connect-timeout";
    /**
     * maximum number of redirects for a client call. Works only if the client has `followingRedirects enabled
     */
    public static final String MAX_REDIRECTS = "io.quarkus.rest.client.max-redirects";
    public static final String READ_TIMEOUT = "io.quarkus.rest.client.read-timeout";

    /**
     * See {@link io.netty.handler.codec.http.multipart.HttpPostRequestEncoder.EncoderMode}, RFC1738 by default
     */
    public static final String MULTIPART_ENCODER_MODE = "io.quarkus.rest.client.multipart-post-encoder-mode";
}
