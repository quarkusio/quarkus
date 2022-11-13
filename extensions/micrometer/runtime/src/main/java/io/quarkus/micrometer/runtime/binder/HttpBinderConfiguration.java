package io.quarkus.micrometer.runtime.binder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jboss.logging.Logger;

import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.quarkus.runtime.LaunchMode;

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
            Pattern defaultIgnore = null;
            String defaultMatch = null;

            if (MicrometerRecorder.httpRootUri.equals(MicrometerRecorder.nonApplicationUri)) {
                // we can't set the default ignore in this case, as the paths overlap
            } else if (serverConfig.suppressNonApplicationUris) {
                defaultIgnore = Pattern.compile(MicrometerRecorder.nonApplicationUri + ".*");
            }

            if (defaultIgnore == null && LaunchMode.current() == LaunchMode.DEVELOPMENT) {
                // if we aren't straight-up ignoring all nonApplication endpoints
                // create a defaultMatch that will fold all dev console related resources into one meter
                String devRoot = MicrometerRecorder.nonApplicationUri + "dev";
                defaultMatch = devRoot + "/.*=" + devRoot;
            }

            // Handle deprecated/previous vertx properties as well
            serverIgnorePatterns = getIgnorePatterns(
                    serverConfig.ignorePatterns.isPresent() ? serverConfig.ignorePatterns : vertxConfig.ignorePatterns,
                    defaultIgnore);
            serverMatchPatterns = getMatchPatterns(
                    serverConfig.matchPatterns.isPresent() ? serverConfig.matchPatterns : vertxConfig.matchPatterns,
                    defaultMatch);
        }

        if (clientEnabled) {
            clientIgnorePatterns = getIgnorePatterns(clientConfig.ignorePatterns, null);
            clientMatchPatterns = getMatchPatterns(clientConfig.matchPatterns, null);
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

    List<Pattern> getIgnorePatterns(Optional<List<String>> configInput, Pattern defaultIgnore) {
        if (configInput.isPresent()) {
            List<String> input = configInput.get();
            List<Pattern> ignorePatterns = new ArrayList<>(input.size() + (defaultIgnore == null ? 0 : 1));
            for (String s : input) {
                ignorePatterns.add(Pattern.compile(s.trim()));
            }
            if (defaultIgnore != null) {
                ignorePatterns.add(defaultIgnore);
            }
            return Collections.unmodifiableList(ignorePatterns);
        }
        if (defaultIgnore != null) {
            return Collections.singletonList(defaultIgnore);
        }
        return Collections.emptyList();
    }

    Map<Pattern, String> getMatchPatterns(Optional<List<String>> configInput, String defaultMatch) {
        if (configInput.isPresent()) {
            List<String> input = configInput.get();
            Map<Pattern, String> matchPatterns = new LinkedHashMap<>(input.size() + (defaultMatch == null ? 0 : 1));
            for (String s : input) {
                parseMatchPattern(s, matchPatterns);
            }
            if (defaultMatch != null) {
                parseMatchPattern(defaultMatch, matchPatterns);
            }
            return Collections.unmodifiableMap(matchPatterns);
        }
        if (defaultMatch != null) {
            Map<Pattern, String> matchPatterns = new HashMap(1);
            parseMatchPattern(defaultMatch, matchPatterns);
            return Collections.unmodifiableMap(matchPatterns);
        }
        return Collections.emptyMap();
    }

    private void parseMatchPattern(String s, Map<Pattern, String> matchPatterns) {
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

    public String getHttpServerRequestsName() {
        return "http.server.requests";
    }

    public String getHttpServerActiveRequestsName() {
        return "http.server.active.requests";
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
