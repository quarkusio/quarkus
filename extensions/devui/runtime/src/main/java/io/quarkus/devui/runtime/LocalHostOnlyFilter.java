package io.quarkus.devui.runtime;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class LocalHostOnlyFilter implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(LocalHostOnlyFilter.class);

    private static final String LOCAL_HOST = "localhost";
    private static final String LOCAL_HOST_IP = "127.0.0.1";

    private final List<String> hosts;
    private final List<Pattern> hostsPatterns;
    private final boolean allowLoopbackHostnames;
    private final LoopbackHostnameResolver loopbackHostnameResolver;

    public LocalHostOnlyFilter(List<String> hosts) {
        this(hosts, false);
    }

    public LocalHostOnlyFilter(List<String> hosts, boolean allowLoopbackHostnames) {
        this.hosts = hosts;
        this.hostsPatterns = DevUIFilterHelper.detectPatterns(this.hosts);
        this.allowLoopbackHostnames = allowLoopbackHostnames;
        this.loopbackHostnameResolver = allowLoopbackHostnames ? new LoopbackHostnameResolver() : null;
    }

    @Override
    public void handle(RoutingContext event) {
        String host = requestHost(event);
        if (host != null && hostIsValid(host)) {
            event.next();
        } else if (host != null && allowLoopbackHostnames) {
            loopbackHostnameResolver.isLoopback(host, event.vertx(), new Handler<Boolean>() {
                @Override
                public void handle(Boolean loopback) {
                    if (loopback) {
                        event.next();
                    } else {
                        reject(event, host);
                    }
                }
            });
        } else {
            reject(event, host);
        }
    }

    private static void reject(RoutingContext event, String host) {
        if (host != null) {
            LOG.errorf("Dev UI: Only localhost is allowed, unexpected host: %s", host);
        }
        HttpServerResponse response = event.response();
        response.setStatusCode(403);
        response.setStatusMessage("Dev UI: Only localhost is allowed - Invalid host");
        response.end();
    }

    private static String requestHost(RoutingContext event) {
        try {
            URI uri = new URI(event.request().absoluteURI());
            URL url = uri.toURL();
            return url.getHost();
        } catch (MalformedURLException | URISyntaxException e) {
            LOG.error("Error while checking if Dev UI is localhost", e);
            return null;
        }
    }

    private boolean hostIsValid(String host) {
        if (host.equals(LOCAL_HOST) || host.equals(LOCAL_HOST_IP)) {
            return true;
        } else if (this.hosts != null && this.hosts.contains(host)) {
            return true;
        } else if (this.hostsPatterns != null && !this.hostsPatterns.isEmpty()) {
            // Regex
            for (Pattern pat : this.hostsPatterns) {
                Matcher matcher = pat.matcher(host);
                if (matcher.matches()) {
                    return true;
                }
            }
        }
        return false;
    }
}
