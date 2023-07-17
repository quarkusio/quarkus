package io.quarkus.resteasy.links.runtime.hal;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;

import org.jboss.resteasy.links.LinksProvider;
import org.jboss.resteasy.links.RESTServiceDiscovery;

import io.quarkus.hal.HalLink;
import io.quarkus.hal.HalService;

@RequestScoped
public class ResteasyHalService extends HalService {

    @Override
    protected Map<String, HalLink> getClassLinks(Class<?> entityClass) {
        return linksToMap(LinksProvider.getClassLinksProvider().getLinks(entityClass,
                Thread.currentThread().getContextClassLoader()));
    }

    @Override
    protected Map<String, HalLink> getInstanceLinks(Object entity) {
        return linksToMap(LinksProvider.getObjectLinksProvider().getLinks(entity,
                Thread.currentThread().getContextClassLoader()));
    }

    private Map<String, HalLink> linksToMap(RESTServiceDiscovery serviceDiscovery) {
        Map<String, HalLink> links = new HashMap<>(serviceDiscovery.size());
        for (RESTServiceDiscovery.AtomLink atomLink : serviceDiscovery) {
            links.put(atomLink.getRel(), new HalLink(atomLink.getHref()));
        }
        return links;
    }
}
