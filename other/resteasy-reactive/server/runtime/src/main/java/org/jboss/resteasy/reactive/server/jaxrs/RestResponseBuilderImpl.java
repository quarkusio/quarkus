package org.jboss.resteasy.reactive.server.jaxrs;

import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.core.HttpHeaders;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.jaxrs.AbstractRestResponseBuilder;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;

public class RestResponseBuilderImpl<T> extends AbstractRestResponseBuilder<T> {

    @Override
    public RestResponse.ResponseBuilder<T> location(URI location) {
        if (location == null) {
            metadata.remove(HttpHeaders.LOCATION);
            return this;
        }
        if (!location.isAbsolute()) {
            // FIXME: this leaks server stuff onto the client
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            if (request != null) {
                ServerHttpRequest req = request.serverRequest();
                try {
                    String host = req.getRequestHost();
                    int port = -1;
                    int index = host.indexOf(":");
                    if (index > -1) {
                        port = Integer.parseInt(host.substring(index + 1));
                        host = host.substring(0, index);
                    }
                    String prefix = "";
                    Deployment deployment = request.getDeployment();
                    if (deployment != null) {
                        // prefix is already sanitised
                        prefix = deployment.getPrefix();
                    }
                    // Spec says relative to request, but TCK tests relative to Base URI, so we do that
                    location = new URI(req.getRequestScheme(), null, host, port,
                            prefix +
                                    (location.getPath().startsWith("/") ? location.getPath() : "/" + location.getPath()),
                            location.getQuery(), null);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        metadata.putSingle(HttpHeaders.LOCATION, location);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> contentLocation(URI location) {
        if (location == null) {
            metadata.remove(HttpHeaders.CONTENT_LOCATION);
            return this;
        }
        if (!location.isAbsolute()) {
            ResteasyReactiveRequestContext request = CurrentRequestManager.get();
            if (request != null) {
                // FIXME: this leaks server stuff onto the client
                ServerHttpRequest req = request.serverRequest();
                try {
                    String host = req.getRequestHost();
                    int port = -1;
                    int index = host.indexOf(":");
                    if (index > -1) {
                        port = Integer.parseInt(host.substring(index + 1));
                        host = host.substring(0, index);
                    }
                    location = new URI(req.getRequestScheme(), null, host, port,
                            location.getPath().startsWith("/") ? location.getPath() : "/" + location.getPath(),
                            location.getQuery(), null);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        metadata.putSingle(HttpHeaders.CONTENT_LOCATION, location);
        return this;
    }

    @Override
    protected AbstractRestResponseBuilder<T> doClone() {
        return new RestResponseBuilderImpl<>();
    }
}
