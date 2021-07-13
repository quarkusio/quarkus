package io.quarkus.micrometer.runtime.binder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jboss.logging.Logger;

import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;

/**
 * Digest configuration options for http metrics once, so they can
 * be used by different binders emitting http metrics (depending on
 * other extension configuration).
 *
 * This is a synthetic bean created at runtime init (see MicrometerRecorder),
 * it cannot be referenced during build or static initialization.
 */
public class HttpBinderConfiguration {
    private static final Logger log = Logger.getLogger(HttpBinderConfiguration.class);

    boolean serverEnabled = true;
    boolean clientEnabled = true;

    List<Pattern> serverIgnorePatterns = Collections.emptyList();
    Map<Pattern, String> serverMatchPatterns = Collections.emptyMap();

    List<Pattern> clientIgnorePatterns = Collections.emptyList();
    Map<Pattern, String> clientMatchPatterns = Collections.emptyMap();

    private HttpBinderConfiguration() {
    }

    @SuppressWarnings("deprecation")
    public HttpBinderConfiguration(boolean httpServerMetrics, boolean httpClientMetrics,
            HttpServerConfig serverConfig, HttpClientConfig clientConfig, VertxConfig vertxConfig) {

        serverEnabled = httpServerMetrics;
        clientEnabled = httpClientMetrics;

        if (serverEnabled) {
            // Handle deprecated/previous vertx properties as well
            serverIgnorePatterns = getIgnorePatterns(
                    serverConfig.ignorePatterns.isPresent() ? serverConfig.ignorePatterns : vertxConfig.ignorePatterns);
            serverMatchPatterns = getMatchPatterns(
                    serverConfig.matchPatterns.isPresent() ? serverConfig.matchPatterns : vertxConfig.matchPatterns);
        }

        if (clientEnabled) {
            clientIgnorePatterns = getIgnorePatterns(clientConfig.ignorePatterns);
            clientMatchPatterns = getMatchPatterns(clientConfig.matchPatterns);
        }
    }

    public boolean isServerEnabled() {
        return serverEnabled;
    }

    public List<Pattern> getServerIgnorePatterns() {
        return serverIgnorePatterns;
    }

    public Map<Pattern, String> getServerMatchPatterns() {
        return serverMatchPatterns;
    }

    public boolean isClientEnabled() {
        return clientEnabled;
    }

    public List<Pattern> getClientIgnorePatterns() {
        return clientIgnorePatterns;
    }

    public Map<Pattern, String> getClientMatchPatterns() {
        return clientMatchPatterns;
    }

    List<Pattern> getIgnorePatterns(Optional<List<String>> configInput) {
        if (configInput.isPresent()) {
            List<String> input = configInput.get();
            List<Pattern> ignorePatterns = new ArrayList<>(input.size());
            for (String s : input) {
                ignorePatterns.add(Pattern.compile(s.trim()));
            }
            return Collections.unmodifiableList(ignorePatterns);
        }
        return Collections.emptyList();
    }

    Map<Pattern, String> getMatchPatterns(Optional<List<String>> configInput) {
        if (configInput.isPresent()) {
            List<String> input = configInput.get();
            Map<Pattern, String> matchPatterns = new LinkedHashMap<>(input.size());
            for (String s : input) {
                int pos = s.indexOf("=");
                if (pos > 0 && s.length() > 2) {
                    String pattern = s.substring(0, pos).trim();
                    String replacement = s.substring(pos + 1).trim();
                    try {
                        matchPatterns.put(Pattern.compile(pattern), replacement);
                    } catch (PatternSyntaxException pse) {
                        log.errorf("Invalid pattern in replacement string (%s=%s): %s", pattern, replacement, pse);
                    }
                } else {
                    log.errorf("Invalid pattern in replacement string (%s). Should be pattern=replacement", s);
                }
            }
            return Collections.unmodifiableMap(matchPatterns);
        }
        return Collections.emptyMap();
    }

    public String getHttpServerRequestsName() {
        return "http.server.requests";
    }

    public String getHttpServerPushName() {
        return "http.server.push";
    }

    public String getHttpServerWebSocketConnectionsName() {
        return "http.server.websocket.connections";
    }

    public String getHttpClientRequestsName() {
        return "http.client.requests";
    }

    public HttpBinderConfiguration unwrap() {
        HttpBinderConfiguration result = new HttpBinderConfiguration();
        // not dev-mode changeable
        result.clientEnabled = this.clientEnabled;
        result.serverEnabled = this.serverEnabled;
        return result.update(this);
    }

    public HttpBinderConfiguration update(HttpBinderConfiguration httpConfig) {
        this.clientMatchPatterns = httpConfig.clientMatchPatterns;
        this.serverMatchPatterns = httpConfig.serverMatchPatterns;
        this.clientIgnorePatterns = httpConfig.clientIgnorePatterns;
        this.serverIgnorePatterns = httpConfig.serverIgnorePatterns;
        return this;
    }
}
