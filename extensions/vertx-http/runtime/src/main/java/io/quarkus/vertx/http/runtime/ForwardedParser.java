/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

// This code was Heavily influenced from spring forward header parser
// https://github.com/spring-projects/spring-framework/blob/main/spring-web/src/main/java/org/springframework/web/util/UriComponentsBuilder.java#L849

package io.quarkus.vertx.http.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.netty.util.AsciiString;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;

class ForwardedParser {
    private static final Logger log = Logger.getLogger(ForwardedParser.class);

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final AsciiString FORWARDED = AsciiString.cached("Forwarded");
    private static final AsciiString X_FORWARDED_SSL = AsciiString.cached("X-Forwarded-Ssl");
    private static final AsciiString X_FORWARDED_PROTO = AsciiString.cached("X-Forwarded-Proto");
    private static final AsciiString X_FORWARDED_PORT = AsciiString.cached("X-Forwarded-Port");
    private static final AsciiString X_FORWARDED_FOR = AsciiString.cached("X-Forwarded-For");
    private static final AsciiString X_FORWARDED_TRUSTED_PROXY = AsciiString.cached("X-Forwarded-Trusted-Proxy");

    private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("host=\"?([^;,\"]+)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("proto=\"?([^;,\"]+)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("for=\"?([^;,\"]+)\"?", Pattern.CASE_INSENSITIVE);

    private final static int PORT_MIN_VALID_VALUE = 0;
    private final static int PORT_MAX_VALID_VALUE = 65535;

    private final HttpServerRequest delegate;
    private final ForwardingProxyOptions forwardingProxyOptions;
    private final TrustedProxyCheck trustedProxyCheck;

    /**
     * Does not use the Netty constant (`host`) to enforce the header name convention.
     */
    private final static AsciiString HOST_HEADER = AsciiString.cached("Host");

    private boolean calculated;
    private String host;
    private int port = -1;
    private String scheme;
    private String uri;
    private String absoluteURI;
    private SocketAddress remoteAddress;

    private HostAndPort authority;

    ForwardedParser(HttpServerRequest delegate, ForwardingProxyOptions forwardingProxyOptions,
            TrustedProxyCheck trustedProxyCheck) {
        this.delegate = delegate;
        this.forwardingProxyOptions = forwardingProxyOptions;
        this.trustedProxyCheck = trustedProxyCheck;
    }

    public String scheme() {
        if (!calculated)
            calculate();
        return scheme;
    }

    String host() {
        if (!calculated)
            calculate();
        return host;
    }

    boolean isSSL() {
        if (!calculated)
            calculate();

        return scheme.equals(HTTPS_SCHEME);
    }

    HostAndPort authority() {
        if (!calculated) {
            calculate();
        }
        return authority;
    }

    String absoluteURI() {
        if (!calculated)
            calculate();

        return absoluteURI;
    }

    SocketAddress remoteAddress() {
        if (!calculated)
            calculate();

        return remoteAddress;
    }

    String uri() {
        if (!calculated)
            calculate();

        return uri;
    }

    private void calculate() {
        calculated = true;
        remoteAddress = delegate.remoteAddress();
        scheme = delegate.scheme();
        setHostAndPort(delegate.host(), port);
        uri = delegate.uri();

        boolean isProxyAllowed = trustedProxyCheck.isProxyAllowed();
        if (isProxyAllowed) {
            if (forwardingProxyOptions.allowForwarded && forwardingProxyOptions.allowXForwarded) {
                Forwarded forwardedHeaders = null;
                Forwarded xForwardedHeaders = null;
                if (ProxyConfig.ForwardedPrecedence.FORWARDED == forwardingProxyOptions.forwardedPrecedence) {
                    // Forwarded values may override X-Forwarded values if strict forwarded control is disabled
                    xForwardedHeaders = processXForwarded();
                    forwardedHeaders = processForwarded();
                } else {
                    // X-Forwarded values may override Forwarded values if strict forwarded control is disabled
                    forwardedHeaders = processForwarded();
                    xForwardedHeaders = processXForwarded();
                }
                if (forwardingProxyOptions.strictForwardedControl) {
                    log.debug(
                            "Strict forwarded control is enabled, checking if common Forwarded and X-Forwarded properties match");
                    if (!xForwardedHeaders.modifiedPropertiesMatch(forwardedHeaders)) {
                        delegate.response().setStatusCode(400);
                        delegate.end();
                        return;
                    }
                    log.debug("Common Forwarded and X-Forwarded properties match");
                }
            } else if (forwardingProxyOptions.allowForwarded) {
                processForwarded();
            } else if (forwardingProxyOptions.allowXForwarded) {
                processXForwarded();
            }
        }

        if (((scheme.equals(HTTP_SCHEME) && port == 80) || (scheme.equals(HTTPS_SCHEME) && port == 443))) {
            port = -1;
        }

        authority = HostAndPort.create(host, port >= 0 ? port : -1);
        host = host + (port >= 0 ? ":" + port : "");
        delegate.headers().set(HOST_HEADER, host);
        if (forwardingProxyOptions.enableTrustedProxyHeader) {
            // Verify that the header was not already set.
            if (delegate.headers().contains(X_FORWARDED_TRUSTED_PROXY)) {
                log.warn("The header " + X_FORWARDED_TRUSTED_PROXY + " was already set. Overwriting it.");
            }
            delegate.headers().set(X_FORWARDED_TRUSTED_PROXY, Boolean.toString(isProxyAllowed));
        } else {
            // Verify that the header was not already set - to avoid forgery.
            if (delegate.headers().contains(X_FORWARDED_TRUSTED_PROXY)) {
                log.warn("The header " + X_FORWARDED_TRUSTED_PROXY + " was already set. Removing it.");
                delegate.headers().remove(X_FORWARDED_TRUSTED_PROXY);
            }
        }

        absoluteURI = scheme + "://" + host + uri;
        log.debugf("Recalculated absoluteURI to %s", absoluteURI);
    }

    private Forwarded processForwarded() {
        Forwarded forwardedValues = new Forwarded();

        String forwarded = delegate.getHeader(FORWARDED);
        if (forwarded == null) {
            return forwardedValues;
        }

        Matcher matcher = FORWARDED_PROTO_PATTERN.matcher(forwarded);
        if (matcher.find()) {
            scheme = matcher.group(1).trim();
            port = -1;
            log.debugf("Using Forwarded 'proto' to set scheme to %s", scheme);
            forwardedValues.setScheme(scheme);
            forwardedValues.setPort(port);
        }

        matcher = FORWARDED_HOST_PATTERN.matcher(forwarded);
        if (matcher.find()) {
            setHostAndPort(matcher.group(1).trim(), port);
            log.debugf("Using Forwarded 'host' to set host to %s and port to %d", host, port);
            forwardedValues.setHost(host);
            forwardedValues.setPort(port);
        }

        matcher = FORWARDED_FOR_PATTERN.matcher(forwarded);
        if (matcher.find()) {
            remoteAddress = parseFor(matcher.group(1).trim(), remoteAddress != null ? remoteAddress.port() : port);
            forwardedValues.setRemoteHost(remoteAddress.host());
            forwardedValues.setRemotePort(remoteAddress.port());
            log.debugf("Using Forwarded 'for' to set for host to %s and for port to %d", remoteAddress.host(),
                    remoteAddress.port());
        }

        return forwardedValues;
    }

    private Forwarded processXForwarded() {
        Forwarded xForwardedValues = new Forwarded();

        String protocolHeader = delegate.getHeader(X_FORWARDED_PROTO);
        if (protocolHeader != null) {
            scheme = getFirstElement(protocolHeader);
            port = -1;
            log.debugf("Using X-Forwarded-Proto to set scheme to %s", scheme);
            xForwardedValues.setScheme(scheme);
            xForwardedValues.setPort(port);
        }

        String forwardedSsl = delegate.getHeader(X_FORWARDED_SSL);
        boolean isForwardedSslOn = forwardedSsl != null && forwardedSsl.equalsIgnoreCase("on");
        if (isForwardedSslOn) {
            scheme = HTTPS_SCHEME;
            port = -1;
            log.debugf("Using X-Forwarded-Ssl to set scheme to %s", scheme);
            xForwardedValues.setScheme(scheme);
            xForwardedValues.setPort(port);
        }

        if (forwardingProxyOptions.enableForwardedHost) {
            String hostHeader = delegate.getHeader(forwardingProxyOptions.forwardedHostHeader);
            if (hostHeader != null) {
                port = -1;
                setHostAndPort(getFirstElement(hostHeader), port);
                log.debugf("Using %s to set host to %s and port to %d", hostHeader, host, port);
                xForwardedValues.setHost(host);
                xForwardedValues.setPort(port);
            }
        }

        if (forwardingProxyOptions.enableForwardedPrefix) {
            String prefixHeader = delegate.getHeader(forwardingProxyOptions.forwardedPrefixHeader);
            if (prefixHeader != null) {
                log.debugf("Using %s to prefix URI %s with prefix %s", forwardingProxyOptions.forwardedPrefixHeader, uri,
                        prefixHeader);
                uri = appendPrefixToUri(prefixHeader, uri);
            }
        }

        String portHeader = delegate.getHeader(X_FORWARDED_PORT);
        if (portHeader != null) {
            port = parsePort(getFirstElement(portHeader), port);
            log.debugf("Using X-Forwarded-Port to set port to %d", port);
            xForwardedValues.setPort(port);
        }

        String forHeader = delegate.getHeader(X_FORWARDED_FOR);
        if (forHeader != null) {
            remoteAddress = parseFor(getFirstElement(forHeader), remoteAddress != null ? remoteAddress.port() : port);
            xForwardedValues.setRemoteHost(remoteAddress.host());
            xForwardedValues.setRemotePort(remoteAddress.port());
            log.debugf("Using X-Forwarded-For to set for host to %s and for port to %d", remoteAddress.host(),
                    remoteAddress.port());
        }

        return xForwardedValues;
    }

    private void setHostAndPort(String hostToParse, int defaultPort) {
        if (hostToParse == null) {
            hostToParse = "";
        }
        String[] hostAndPort = parseHostAndPort(hostToParse);
        host = hostAndPort[0];
        delegate.headers().set(HOST_HEADER, host);
        port = parsePort(hostAndPort[1], defaultPort);
    }

    private SocketAddress parseFor(String forToParse, int defaultPort) {
        String[] hostAndPort = parseHostAndPort(forToParse);
        String host = hostAndPort[0];
        int port = parsePort(hostAndPort[1], defaultPort);
        return new SocketAddressImpl(port, host);
    }

    private String getFirstElement(String value) {
        int index = value.indexOf(',');
        return index == -1 ? value : value.substring(0, index);
    }

    /**
     * Returns a String[] of 2 elements, with the first being the host and the second the port
     */
    private String[] parseHostAndPort(String hostToParse) {
        String[] hostAndPort = { hostToParse, "" };
        int portSeparatorIdx = hostToParse.lastIndexOf(':');
        int squareBracketIdx = hostToParse.lastIndexOf(']');
        if ((squareBracketIdx > -1 && portSeparatorIdx > squareBracketIdx)) {
            // ipv6 with port
            hostAndPort[0] = hostToParse.substring(0, portSeparatorIdx);
            hostAndPort[1] = hostToParse.substring(portSeparatorIdx + 1);
        } else {
            long numberOfColons = hostToParse.chars().filter(ch -> ch == ':').count();
            if (numberOfColons == 1 && !hostToParse.endsWith(":")) {
                // ipv4 with port
                hostAndPort[0] = hostToParse.substring(0, portSeparatorIdx);
                hostAndPort[1] = hostToParse.substring(portSeparatorIdx + 1);
            }
        }
        return hostAndPort;
    }

    private int parsePort(String portToParse, int defaultPort) {
        if (portToParse != null && portToParse.length() > 0) {
            try {
                int port = Integer.parseInt(portToParse);
                if (port < PORT_MIN_VALID_VALUE || port > PORT_MAX_VALID_VALUE) {
                    log.errorf("Failed to validate a port from \"forwarded\"-type headers, using the default port %d",
                            defaultPort);
                    return defaultPort;
                }
                return port;
            } catch (NumberFormatException ignored) {
                log.errorf("Failed to parse a port from \"forwarded\"-type headers, using the default port %d", defaultPort);
            }
        }
        return defaultPort;
    }

    private String appendPrefixToUri(String prefix, String uri) {
        String parsed = stripSlashes(prefix);
        return parsed.isEmpty() ? uri : '/' + parsed + uri;
    }

    private String stripSlashes(String uri) {
        String result;
        if (!uri.isEmpty()) {
            int beginIndex = 0;
            if (uri.startsWith("/")) {
                beginIndex = 1;
            }

            int endIndex = uri.length();
            if (uri.endsWith("/") && uri.length() > 1) {
                endIndex = uri.length() - 1;
            }
            result = uri.substring(beginIndex, endIndex);
        } else {
            result = uri;
        }

        return result;
    }

    static class Forwarded {
        private static String SCHEME = "scheme";
        private static String HOST = "host";
        private static String PORT = "port";
        private static String REMOTE_HOST = "remote host";
        private static String REMOTE_PORT = "remote port";

        private Map<String, Object> forwarded = new HashMap<>();

        public void setScheme(String scheme) {
            forwarded.put(SCHEME, scheme);
        }

        public void setHost(String host) {
            forwarded.put(HOST, host);
        }

        public void setPort(Integer port) {
            forwarded.put(PORT, port);
        }

        public void setRemoteHost(String host) {
            forwarded.put(REMOTE_HOST, host);
        }

        public void setRemotePort(Integer port) {
            forwarded.put(REMOTE_PORT, port);
        }

        public boolean modifiedPropertiesMatch(Forwarded fw) {
            Set<String> keys = new HashSet<>(forwarded.keySet());
            keys.retainAll(fw.forwarded.keySet());

            for (String key : keys) {
                if (!forwarded.get(key).equals(fw.forwarded.get(key))) {
                    log.debugf("Forwarded and X-Forwarded %s values do not match.", key);
                    return false;
                }
            }

            return true;

        }
    }
}
