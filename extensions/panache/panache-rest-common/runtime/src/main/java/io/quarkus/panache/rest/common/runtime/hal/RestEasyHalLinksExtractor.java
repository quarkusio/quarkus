package io.quarkus.panache.rest.common.runtime.hal;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.links.ClassLinksProvider;
import org.jboss.resteasy.links.ObjectLinksProvider;
import org.jboss.resteasy.links.RESTServiceDiscovery;
import org.jboss.resteasy.spi.Registry;

final class RestEasyHalLinksExtractor implements HalLinksExtractor {

    @Override
    public Map<String, HalLink> getLinks(Class<?> entityClass) {
        return fromRestServiceDiscovery(getClassLinksProvider().getLinks(entityClass));
    }

    @Override
    public Map<String, HalLink> getLinks(Object entity) {
        return fromRestServiceDiscovery(getObjectLinksProvider().getLinks(entity));
    }

    private Map<String, HalLink> fromRestServiceDiscovery(RESTServiceDiscovery serviceDiscovery) {
        Map<String, HalLink> links = new HashMap<>(serviceDiscovery.size());

        for (RESTServiceDiscovery.AtomLink atomLink : serviceDiscovery) {
            links.put(atomLink.getRel(), new HalLink(atomLink.getHref()));
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
