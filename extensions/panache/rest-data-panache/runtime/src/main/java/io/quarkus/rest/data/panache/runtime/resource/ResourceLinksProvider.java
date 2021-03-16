package io.quarkus.rest.data.panache.runtime.resource;

import java.util.HashMap;
import java.util.Map;

import org.jboss.resteasy.links.LinksProvider;
import org.jboss.resteasy.links.RESTServiceDiscovery;

public final class ResourceLinksProvider {

    private static final String SELF_REF = "self";

    public Map<String, String> getClassLinks(Class<?> className) {
        RESTServiceDiscovery links = LinksProvider
                .getClassLinksProvider()
                .getLinks(className, Thread.currentThread().getContextClassLoader());
        return linksToMap(links);
    }

    public Map<String, String> getInstanceLinks(Object instance) {
        RESTServiceDiscovery links = LinksProvider
                .getObjectLinksProvider()
                .getLinks(instance, Thread.currentThread().getContextClassLoader());
        return linksToMap(links);
    }

    public String getSelfLink(Object instance) {
        RESTServiceDiscovery.AtomLink link = LinksProvider.getObjectLinksProvider()
                .getLinks(instance, Thread.currentThread().getContextClassLoader())
                .getLinkForRel(SELF_REF);
        return link == null ? null : link.getHref();
    }

    private Map<String, String> linksToMap(RESTServiceDiscovery serviceDiscovery) {
        Map<String, String> links = new HashMap<>(serviceDiscovery.size());
        for (RESTServiceDiscovery.AtomLink atomLink : serviceDiscovery) {
            links.put(atomLink.getRel(), atomLink.getHref());
        }
        return links;
    }
}
