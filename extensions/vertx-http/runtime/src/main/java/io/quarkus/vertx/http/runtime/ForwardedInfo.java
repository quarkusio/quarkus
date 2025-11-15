package io.quarkus.vertx.http.runtime;

/**
 * Contains the information provided by the various forwarding headers
 * This object can be obtained by using looking up {@code ForwardedInfo.CONTEXT_KEY}
 * in the requests local context.
 */
public interface ForwardedInfo {

    String CONTEXT_KEY = ForwardedInfo.class.getName();

    String getScheme();

    String getHost();

    Integer getPort();

    String getRemoteHost();

    Integer getRemotePort();

    String getPrefix();
}
