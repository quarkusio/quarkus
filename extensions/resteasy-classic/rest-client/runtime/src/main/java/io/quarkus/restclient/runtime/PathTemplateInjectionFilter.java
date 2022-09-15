package io.quarkus.restclient.runtime;

import jakarta.annotation.Priority;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

@Priority(Integer.MIN_VALUE)
public class PathTemplateInjectionFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        Object prop = requestContext.getConfiguration().getProperty("UrlPathTemplate");
        if (prop != null) {
            requestContext.setProperty("UrlPathTemplate", prop);
        }
    }
}
