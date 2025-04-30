package io.quarkus.devui.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.quarkus.vertx.http.runtime.cors.CORSFilter;
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
    private static final String HTTP_LOCAL_HOST = "http://" + LOCAL_HOST;
    private static final String HTTPS_LOCAL_HOST = "https://" + LOCAL_HOST;
    private static final String HTTP_LOCAL_HOST_IP = "http://" + LOCAL_HOST_IP;
    private static final String HTTPS_LOCAL_HOST_IP = "https://" + LOCAL_HOST_IP;
    private static final String CHROME_EXTENSION = "chrome-extension://";

    public DevUICORSFilter() {
    }

    private static CORSFilter corsFilter() {
        int httpPort = ConfigProvider.getConfig().getValue(HTTP_PORT_CONFIG_PROP, int.class);
        int httpsPort = ConfigProvider.getConfig().getValue(HTTPS_PORT_CONFIG_PROP, int.class);
        CORSConfig config = new CORSConfig() {
            @Override
            public Optional<List<String>> origins() {
                return Optional.of(List.of(HTTP_LOCAL_HOST + ":" + httpPort, HTTP_LOCAL_HOST_IP + ":" + httpPort,
                        HTTPS_LOCAL_HOST + ":" + httpsPort, HTTPS_LOCAL_HOST_IP + ":" + httpsPort));
            }

            @Override
            public Optional<List<String>> methods() {
                return Optional.empty();
            }

            @Override
            public Optional<List<String>> headers() {
                return Optional.empty();
            }

            @Override
            public Optional<List<String>> exposedHeaders() {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> accessControlMaxAge() {
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> accessControlAllowCredentials() {
                return Optional.empty();
            }
        };
        return new CORSFilter(config);
    }

    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest request = event.request();
        HttpServerResponse response = event.response();
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            corsFilter().handle(event);
        } else {
            if (origin.startsWith(HTTP_LOCAL_HOST) || origin.startsWith(HTTPS_LOCAL_HOST)
                    || origin.startsWith(HTTP_LOCAL_HOST_IP) || origin.startsWith(HTTPS_LOCAL_HOST_IP)) {
                corsFilter().handle(event);
            } else {
                if (!origin.startsWith(CHROME_EXTENSION)) {
                    LOG.errorf("Only localhost origin is allowed, but Origin header value is: %s", origin);
                }
                response.setStatusCode(403);
                response.setStatusMessage("CORS Rejected - Invalid origin");
                response.end();
            }
        }
    }
}
