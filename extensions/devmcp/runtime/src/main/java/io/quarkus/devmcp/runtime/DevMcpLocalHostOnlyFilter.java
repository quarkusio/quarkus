package io.quarkus.devmcp.runtime;

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

public class DevMcpLocalHostOnlyFilter implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(DevMcpLocalHostOnlyFilter.class);

    private static final String LOCAL_HOST = "localhost";
    private static final String LOCAL_HOST_IP = "127.0.0.1";

    private final List<String> hosts;
    private final List<Pattern> hostsPatterns;

    public DevMcpLocalHostOnlyFilter(List<String> hosts) {
        this.hosts = hosts;
        this.hostsPatterns = DevMcpFilterHelper.detectPatterns(this.hosts);
    }

    @Override
    public void handle(RoutingContext event) {
        HttpServerResponse response = event.response();
        if (hostIsValid(event)) {
            event.next();
        } else {
            response.setStatusCode(403);
            response.setStatusMessage("Dev MCP: Only localhost is allowed - Invalid host");
            response.end();
        }
    }

    private boolean hostIsValid(RoutingContext event) {
        try {
            URI uri = new URI(event.request().absoluteURI());
            URL url = uri.toURL();
            String host = url.getHost();

            if (host.equals(LOCAL_HOST) || host.equals(LOCAL_HOST_IP)) {
                return true;
            } else if (this.hosts != null && this.hosts.contains(host)) {
                return true;
            } else if (this.hostsPatterns != null && !this.hostsPatterns.isEmpty()) {
                for (Pattern pat : this.hostsPatterns) {
                    Matcher matcher = pat.matcher(host);
                    if (matcher.matches()) {
                        return true;
                    }
                }
            }
            LOG.errorf("Dev MCP: Only localhost is allowed, unexpected host: %s", host);
            return false;
        } catch (MalformedURLException | URISyntaxException e) {
            LOG.error("Error while checking if Dev MCP host is localhost", e);
        }
        return false;
    }
}
