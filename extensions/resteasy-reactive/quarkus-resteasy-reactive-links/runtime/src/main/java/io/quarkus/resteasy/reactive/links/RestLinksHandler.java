package io.quarkus.resteasy.reactive.links;

import java.util.Collection;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;

public class RestLinksHandler implements ServerRestHandler {

    private RestLinkData restLinkData;

    public RestLinkData getRestLinkData() {
        return restLinkData;
    }

    public void setRestLinkData(RestLinkData restLinkData) {
        this.restLinkData = restLinkData;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext context) {
        Response response = context.getResponse().get();
        for (Link link : getLinks(response)) {
            response.getHeaders().add("Link", link);
        }
    }

    private Collection<Link> getLinks(Response response) {
        if ((restLinkData.getRestLinkType() == RestLinkType.INSTANCE) && response.hasEntity()) {
            return getTestLinksProvider().getInstanceLinks(response.getEntity());
        }
        return getTestLinksProvider()
                .getTypeLinks(restLinkData.getEntityType() != null ? entityTypeClass() : response.getEntity().getClass());
    }

    private Class<?> entityTypeClass() {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(restLinkData.getEntityType());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable load class '" + restLinkData.getEntityType() + "'", e);
        }
    }

    private RestLinksProvider getTestLinksProvider() {
        return Arc.container().instance(RestLinksProvider.class).get();
    }

    public static class RestLinkData {

        public RestLinkData(RestLinkType restLinkType, String entityType) {
            this.restLinkType = restLinkType;
            this.entityType = entityType;
        }

        public RestLinkData() {
        }

        private RestLinkType restLinkType;
        private String entityType;

        public RestLinkType getRestLinkType() {
            return restLinkType;
        }

        public void setRestLinkType(RestLinkType restLinkType) {
            this.restLinkType = restLinkType;
        }

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }
    }
}
