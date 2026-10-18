package io.quarkus.it.keycloak;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
public class RestClientContentTypeRequestFilter implements ClientRequestFilter {
    private static final Logger log = Logger.getLogger(RestClientContentTypeRequestFilter.class);

    @Override
    public void filter(ClientRequestContext rc) throws IOException {
        if ("POST".equals(rc.getMethod())) {
            log.debugf("Content-Type is set to %s, replacing it with text/plain", rc.getHeaderString("Content-Type"));
            rc.getHeaders().putSingle("Content-Type", "text/plain");
        }
    }

}
