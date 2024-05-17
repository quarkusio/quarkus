package io.quarkus.websockets.next.runtime;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.websockets.next.WebSocketClientException;
import io.quarkus.websockets.next.WebSocketsClientRuntimeConfig;
import io.vertx.core.Vertx;

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

    WebSocketConnectorBase(Vertx vertx, Codecs codecs,
            ClientConnectionManager connectionManager, WebSocketsClientRuntimeConfig config) {
        this.headers = new HashMap<>();
        this.subprotocols = new HashSet<>();
        this.pathParams = new HashMap<>();
        this.vertx = vertx;
        this.codecs = codecs;
        this.connectionManager = connectionManager;
        this.config = config;
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
            m.appendReplacement(sb, val);
        }
        m.appendTail(sb);
        return path.startsWith("/") ? sb.toString() : "/" + sb.toString();
    }

}
