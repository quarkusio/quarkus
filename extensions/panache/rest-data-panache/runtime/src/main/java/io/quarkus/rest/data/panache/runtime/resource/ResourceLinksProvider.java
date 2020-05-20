package io.quarkus.rest.data.panache.runtime.resource;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.links.ClassLinksProvider;
import org.jboss.resteasy.links.ObjectLinksProvider;
import org.jboss.resteasy.links.RESTServiceDiscovery;
import org.jboss.resteasy.spi.Registry;

public final class ResourceLinksProvider {

    private static final String SELF_REF = "self";

    public Map<String, String> getClassLinks(Class<?> className) {
        return linksToMap(getClassLinksProvider().getLinks(className));
    }

    public Map<String, String> getInstanceLinks(Object instance) {
        return linksToMap(getObjectLinksProvider().getLinks(instance));
    }

    public String getSelfLink(Object instance) {
        RESTServiceDiscovery.AtomLink link = getObjectLinksProvider()
                .getLinks(instance)
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

    private ObjectLinksProvider getObjectLinksProvider() {
        UriInfo uriInfo = ResteasyContext.getContextData(UriInfo.class);
        ResourceMethodRegistry registry = (ResourceMethodRegistry) ResteasyContext.getContextData(Registry.class);
        return new ObjectLinksProvider(uriInfo, registry);
    }

    private ClassLinksProvider getClassLinksProvider() {
        UriInfo uriInfo = ResteasyContext.getContextData(UriInfo.class);
        ResourceMethodRegistry registry = (ResourceMethodRegistry) ResteasyContext.getContextData(Registry.class);
        return new ClassLinksProvider(uriInfo, registry);
    }
}
