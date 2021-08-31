package io.quarkus.rest.data.panache.runtime.resource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Link;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.resteasy.reactive.links.RestLinksProvider;

public final class ResourceLinksProvider {

    private static final String SELF_REF = "self";

    public Map<String, String> getClassLinks(Class<?> className) {
        return linksToMap(restLinksProvider().getTypeLinks(className));
    }

    public Map<String, String> getInstanceLinks(Object instance) {
        return linksToMap(restLinksProvider().getInstanceLinks(instance));
    }

    public String getSelfLink(Object instance) {
        Collection<Link> links = restLinksProvider().getInstanceLinks(instance);
        for (Link link : links) {
            if (SELF_REF.equals(link.getRel())) {
                return link.getUri().toString();
            }
        }
        return null;
    }

    private RestLinksProvider restLinksProvider() {
        InstanceHandle<RestLinksProvider> instance = Arc.container().instance(RestLinksProvider.class);
        if (instance.isAvailable()) {
            return instance.get();
        }
        throw new IllegalStateException("Invalid use of '" + this.getClass().getName()
                + "'. No request scope bean found for type '" + ResourceLinksProvider.class.getName() + "'");
    }

    private Map<String, String> linksToMap(Collection<Link> links) {
        Map<String, String> result = new HashMap<>();
        for (Link link : links) {
            result.put(link.getRel(), link.getUri().toString());
        }
        return result;
    }
}
