package io.quarkus.resteasy.reactive.links.runtime.hal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Link;

import io.quarkus.hal.HalLink;
import io.quarkus.hal.HalService;
import io.quarkus.resteasy.reactive.links.RestLinksProvider;

@RequestScoped
public class ResteasyReactiveHalService extends HalService {
    private final RestLinksProvider linksProvider;

    @Inject
    public ResteasyReactiveHalService(RestLinksProvider linksProvider) {
        this.linksProvider = linksProvider;
    }

    @Override
    protected Map<String, HalLink> getClassLinks(Class<?> entityType) {
        return linksToMap(linksProvider.getTypeLinks(entityType));
    }

    @Override
    protected Map<String, HalLink> getInstanceLinks(Object entity) {
        return linksToMap(linksProvider.getInstanceLinks(entity));
    }

    private Map<String, HalLink> linksToMap(Collection<Link> refLinks) {
        Map<String, HalLink> links = new HashMap<>();
        for (Link link : refLinks) {
            links.put(link.getRel(), new HalLink(link.getUri().toString()));
        }

        return links;
    }
}
