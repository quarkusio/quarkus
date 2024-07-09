package io.quarkus.websockets.next.runtime;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.websockets.next.WebSocketClientException;
import io.quarkus.websockets.next.WebSocketsClientRuntimeConfig;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.net.SSLOptions;

abstract class WebSocketConnectorBase<THIS extends WebSocketConnectorBase<THIS>> {

    protected static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{[a-zA-Z0-9_]+\\}");

    // mutable state

    protected URI baseUri;

    protected final Map<String, String> pathParams;

    protected final Map<String, List<String>> headers;

    protected final Set<String> subprotocols;

    protected String path;

    protected Set<String> pathParamNames;

    // injected dependencies

    protected final Vertx vertx;

    protected final Codecs codecs;

    protected final ClientConnectionManager connectionManager;

    protected final WebSocketsClientRuntimeConfig config;

    protected final TlsConfigurationRegistry tlsConfigurationRegistry;

    WebSocketConnectorBase(Vertx vertx, Codecs codecs,
            ClientConnectionManager connectionManager, WebSocketsClientRuntimeConfig config,
            TlsConfigurationRegistry tlsConfigurationRegistry) {
        this.headers = new HashMap<>();
        this.subprotocols = new HashSet<>();
        this.pathParams = new HashMap<>();
        this.vertx = vertx;
        this.codecs = codecs;
        this.connectionManager = connectionManager;
        this.config = config;
        this.tlsConfigurationRegistry = tlsConfigurationRegistry;
        this.path = "";
        this.pathParamNames = Set.of();
    }

    public THIS baseUri(URI baseUri) {
        this.baseUri = Objects.requireNonNull(baseUri);
        return self();
    }

    public THIS addHeader(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        List<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        }
        values.add(value);
        return self();
    }

    public THIS pathParam(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        if (!pathParamNames.contains(name)) {
            throw new IllegalArgumentException(
                    String.format("[%s] is not a valid path parameter in the path %s", name, path));
        }
        pathParams.put(name, value);
        return self();
    }

    public THIS addSubprotocol(String value) {
        subprotocols.add(Objects.requireNonNull(value));
        return self();
    }

    void setPath(String path) {
        this.path = Objects.requireNonNull(path);
        this.pathParamNames = getPathParamNames(path);
    }

    @SuppressWarnings("unchecked")
    protected THIS self() {
        return (THIS) this;
    }

    Set<String> getPathParamNames(String path) {
        Set<String> names = new HashSet<>();
        Matcher m = PATH_PARAM_PATTERN.matcher(path);
        while (m.find()) {
            String match = m.group();
            String paramName = match.substring(1, match.length() - 1);
            names.add(paramName);
        }
        return names;
    }

    String replacePathParameters(String path) {
        StringBuilder sb = new StringBuilder();
        Matcher m = PATH_PARAM_PATTERN.matcher(path);
        while (m.find()) {
            // Replace {foo} with the param value
            String match = m.group();
            String paramName = match.substring(1, match.length() - 1);
            String val = pathParams.get(paramName);
            if (val == null) {
                throw new WebSocketClientException("Unable to obtain the path param for: " + paramName);
            }
            m.appendReplacement(sb, URLEncoder.encode(val, StandardCharsets.UTF_8));
        }
        m.appendTail(sb);
        return path.startsWith("/") ? sb.toString() : "/" + sb.toString();
    }

    protected WebSocketClientOptions populateClientOptions() {
        WebSocketClientOptions clientOptions = new WebSocketClientOptions();
        if (config.offerPerMessageCompression()) {
            clientOptions.setTryUsePerMessageCompression(true);
            if (config.compressionLevel().isPresent()) {
                clientOptions.setCompressionLevel(config.compressionLevel().getAsInt());
            }
        }
        if (config.maxMessageSize().isPresent()) {
            clientOptions.setMaxMessageSize(config.maxMessageSize().getAsInt());
        }

        Optional<TlsConfiguration> maybeTlsConfiguration = TlsConfiguration.from(tlsConfigurationRegistry,
                config.tlsConfigurationName());
        if (maybeTlsConfiguration.isPresent()) {
            clientOptions.setSsl(true);

            TlsConfiguration tlsConfiguration = maybeTlsConfiguration.get();
            if (tlsConfiguration.getTrustStoreOptions() != null) {
                clientOptions.setTrustOptions(tlsConfiguration.getTrustStoreOptions());
            }

            // For mTLS:
            if (tlsConfiguration.getKeyStoreOptions() != null) {
                clientOptions.setKeyCertOptions(tlsConfiguration.getKeyStoreOptions());
            }

            if (tlsConfiguration.isTrustAll()) {
                clientOptions.setTrustAll(true);
            }
            if (tlsConfiguration.getHostnameVerificationAlgorithm().isPresent()
                    && tlsConfiguration.getHostnameVerificationAlgorithm().get().equals("NONE")) {
                // Only disable hostname verification if the algorithm is explicitly set to NONE
                clientOptions.setVerifyHost(false);
            }

            SSLOptions sslOptions = tlsConfiguration.getSSLOptions();
            if (sslOptions != null) {
                clientOptions.setSslHandshakeTimeout(sslOptions.getSslHandshakeTimeout());
                clientOptions.setSslHandshakeTimeoutUnit(sslOptions.getSslHandshakeTimeoutUnit());
                for (String suite : sslOptions.getEnabledCipherSuites()) {
                    clientOptions.addEnabledCipherSuite(suite);
                }
                for (Buffer buffer : sslOptions.getCrlValues()) {
                    clientOptions.addCrlValue(buffer);
                }
                clientOptions.setEnabledSecureTransportProtocols(sslOptions.getEnabledSecureTransportProtocols());
                clientOptions.setUseAlpn(sslOptions.isUseAlpn());
            }
        }
        return clientOptions;
    }

    protected WebSocketConnectOptions newConnectOptions(URI serverEndpointUri) {
        WebSocketConnectOptions connectOptions = new WebSocketConnectOptions()
                .setSsl(isHttps(serverEndpointUri))
                .setHost(serverEndpointUri.getHost());
        if (serverEndpointUri.getPort() != -1) {
            connectOptions.setPort(serverEndpointUri.getPort());
        } else if (isHttps(serverEndpointUri)) {
            // If port is undefined and https is used then use 443 by default
            connectOptions.setPort(443);
        }
        return connectOptions;
    }

    protected boolean isHttps(URI uri) {
        return "https".equals(uri.getScheme());
    }
}
