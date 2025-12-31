package io.quarkus.rest.client.reactive.runtime;

public class ProxyAddressUtil {

    public static HostAndPort parseAddress(String proxyString) {
        int lastColonIndex = proxyString.lastIndexOf(':');

        if (lastColonIndex <= 0 || lastColonIndex == proxyString.length() - 1) {
            throw new RuntimeException("Invalid proxy string. Expected <hostname>:<port>, found '" + proxyString + "'");
        }

        String host = proxyString.substring(0, lastColonIndex);
        int port;
        try {
            port = Integer.parseInt(proxyString.substring(lastColonIndex + 1));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid proxy setting. The port is not a number in '" + proxyString + "'", e);
        }
        return new HostAndPort(host, port);
    }

    public record HostAndPort(String host, int port) {
    }
}
