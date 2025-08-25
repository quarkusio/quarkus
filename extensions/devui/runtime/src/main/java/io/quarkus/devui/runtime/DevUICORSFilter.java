package io.quarkus.devui.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.quarkus.vertx.http.runtime.cors.CORSFilter;
import io.quarkus.vertx.http.security.CORS;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class DevUICORSFilter implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(DevUICORSFilter.class);

    private static final String HTTP_PORT_CONFIG_PROP = "quarkus.http.port";
    private static final String HTTPS_PORT_CONFIG_PROP = "quarkus.http.ssl-port";
    private static final String LOCAL_HOST = "localhost";
    private static final String LOCAL_HOST_IP = "127.0.0.1";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String HTTP_LOCAL_HOST = HTTP + LOCAL_HOST;
    private static final String HTTPS_LOCAL_HOST = HTTPS + LOCAL_HOST;
    private static final String HTTP_LOCAL_HOST_IP = HTTP + LOCAL_HOST_IP;
    private static final String HTTPS_LOCAL_HOST_IP = HTTPS + LOCAL_HOST_IP;
    private static final String CHROME_EXTENSION = "chrome-extension://";

    private final List<String> hosts;
    private final List<Pattern> hostsPatterns;
    private volatile CORSConfig baseCorsConfig;

    public DevUICORSFilter(List<String> hosts) {
        this.hosts = hosts;
        this.hostsPatterns = DevUIFilterHelper.detectPatterns(this.hosts);
        this.baseCorsConfig = null;
    }

    private CORSFilter corsFilter(String allowedHost) {
        final CORSConfig corsConfig;
        if (allowedHost == null) {
            corsConfig = getBaseCorsConfig();
        } else {
            corsConfig = createCorsConfig(allowedHost);
        }
        return new CORSFilter(corsConfig);
    }

    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest request = event.request();
        HttpServerResponse response = event.response();
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || isLocalHost(origin)) {
            corsFilter(null).handle(event);
        } else if (isConfiguredHost(origin) || isConfiguredHostPattern(origin)) {
            corsFilter(origin).handle(event);
        } else {
            if (!origin.startsWith(CHROME_EXTENSION)) {
                LOG.errorf("Only localhost origin is allowed, but Origin header value is: %s", origin);
            }
            response.setStatusCode(403);
            response.setStatusMessage("CORS Rejected - Invalid origin");
            response.end();
        }
    }

    private boolean isLocalHost(String origin) {
        return origin.startsWith(HTTP_LOCAL_HOST) || origin.startsWith(HTTPS_LOCAL_HOST)
                || origin.startsWith(HTTP_LOCAL_HOST_IP) || origin.startsWith(HTTPS_LOCAL_HOST_IP);
    }

    private boolean isConfiguredHost(String origin) {
        if (this.hosts != null) {
            for (String configuredHost : this.hosts) {
                if (origin.startsWith(HTTP + configuredHost) ||
                        origin.startsWith(HTTPS + configuredHost)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isConfiguredHostPattern(String origin) {
        if (this.hostsPatterns != null && !this.hostsPatterns.isEmpty()) {
            // Regex
            for (Pattern pat : this.hostsPatterns) {
                Matcher matcher = pat.matcher(origin);
                if (matcher.matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private CORSConfig getBaseCorsConfig() {
        if (baseCorsConfig == null) {
            int httpPort = ConfigProvider.getConfig().getValue(HTTP_PORT_CONFIG_PROP, int.class);
            int httpsPort = ConfigProvider.getConfig().getValue(HTTPS_PORT_CONFIG_PROP, int.class);
            baseCorsConfig = (CORSConfig) CORS.origins(Set.of(
                    HTTP_LOCAL_HOST + ":" + httpPort,
                    HTTPS_LOCAL_HOST + ":" + httpsPort,
                    HTTP_LOCAL_HOST_IP + ":" + httpPort,
                    HTTPS_LOCAL_HOST_IP + ":" + httpsPort)).build();
        }
        return baseCorsConfig;
    }

    private CORSConfig createCorsConfig(String allowedHost) {
        CORSConfig baseCorsconfig = getBaseCorsConfig();
        List<String> listOfOrigins = new ArrayList<>(baseCorsconfig.origins().get());
        listOfOrigins.add(allowedHost);
        Optional<List<String>> origins = Optional.of(Collections.unmodifiableList(listOfOrigins));
        return new CORSConfig() {

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public Optional<List<String>> origins() {
                return origins;
            }

            @Override
            public Optional<List<String>> methods() {
                return baseCorsconfig.methods();
            }

            @Override
            public Optional<List<String>> headers() {
                return baseCorsConfig.headers();
            }

            @Override
            public Optional<List<String>> exposedHeaders() {
                return baseCorsConfig.exposedHeaders();
            }

            @Override
            public Optional<Duration> accessControlMaxAge() {
                return baseCorsConfig.accessControlMaxAge();
            }

            @Override
            public Optional<Boolean> accessControlAllowCredentials() {
                return baseCorsConfig.accessControlAllowCredentials();
            }
        };
    }
}
