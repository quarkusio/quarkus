package org.jboss.resteasy.reactive.server.spi;

/**
 * Contains the information provided by the various forwarding headers
 */
public interface ForwardedInfo {

    String getScheme();

    String getHost();

    Integer getPort();

    String getRemoteHost();

    Integer getRemotePort();

    String getPrefix();
}
