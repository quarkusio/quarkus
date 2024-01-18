package org.jboss.resteasy.reactive.client.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceInstance;

@Priority(Priorities.AUTHENTICATION)
@Provider
public class StorkClientRequestFilter implements ResteasyReactiveClientRequestFilter {
    private static final Logger log = Logger.getLogger(StorkClientRequestFilter.class);

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        URI uri = requestContext.getUri();
        if (uri != null && uri.getScheme() != null && uri.getScheme().startsWith(Stork.STORK)) {
            String serviceName = uri.getHost();
            if (serviceName == null) { // invalid URI
                throw new IllegalArgumentException("Invalid REST Client URL used: '" + uri + "'");
            }

            requestContext.suspend();
            Uni<ServiceInstance> serviceInstance;
            boolean measureTime = shouldMeasureTime(requestContext.getResponseType());
            try {
                serviceInstance = Stork.getInstance()
                        .getService(serviceName)
                        .selectInstanceAndRecordStart(measureTime);
            } catch (Throwable e) {
                log.error("Error selecting service instance for serviceName: " + serviceName, e);
                requestContext.resume(e);
                return;
            }

            serviceInstance.subscribe()
                    .with(instance -> {
                        boolean isHttps = instance.isSecure() || "storks".equals(uri.getScheme());
                        String scheme = isHttps ? "https" : "http";
                        try {
                            // In the case the service instance does not set the host and/or port
                            String host = instance.getHost() == null ? "localhost" : instance.getHost();
                            int port = instance.getPort();
                            if (instance.getPort() == 0) {
                                if (isHttps) {
                                    port = 433;
                                } else {
                                    port = 80;
                                }
                            }
                            // Service instance can also contain an optional path.
                            Optional<String> path = instance.getPath();
                            String actualPath = uri.getRawPath();
                            if (path.isPresent()) {
                                var p = path.get();
                                if (!p.startsWith("/")) {
                                    p = "/" + p;
                                }
                                if (actualPath == null) {
                                    actualPath = p;
                                } else {
                                    // Append both.
                                    if (actualPath.startsWith("/") || p.endsWith("/")) {
                                        actualPath = p + actualPath;
                                    } else {
                                        actualPath = p + "/" + actualPath;
                                    }
                                }
                            }
                            //To avoid the path double encoding we create uri with path=null and set the path after
                            URI newUri = new URI(scheme,
                                    uri.getUserInfo(), host, port,
                                    null, uri.getQuery(), uri.getFragment());
                            URI build = UriBuilder.fromUri(newUri).path(actualPath).build();
                            requestContext.setUri(build);
                            if (measureTime && instance.gatherStatistics()) {
                                requestContext.setCallStatsCollector(instance);
                            }
                            requestContext.resume();
                        } catch (URISyntaxException e) {
                            requestContext.resume(new IllegalArgumentException("Invalid URI", e));
                        }
                    },
                            requestContext::resume);
        }

    }

    private boolean shouldMeasureTime(GenericType<?> responseType) {
        return !Multi.class.equals(responseType.getRawType());
    }
}
