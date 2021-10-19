package io.quarkus.rest.data.panache.runtime.hal;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.rest.data.panache.runtime.resource.ResourceLinksProvider;

final class RestEasyHalLinksProvider implements HalLinksProvider {

    @Override
    public Map<String, HalLink> getLinks(Class<?> entityClass) {
        return toHalLinkMap(restLinksProvider().getClassLinks(entityClass));
    }

    @Override
    public Map<String, HalLink> getLinks(Object entity) {
        return toHalLinkMap(restLinksProvider().getInstanceLinks(entity));
    }

    private Map<String, HalLink> toHalLinkMap(Map<String, String> links) {
        Map<String, HalLink> halLinks = new HashMap<>(links.size());
        for (Map.Entry<String, String> entry : links.entrySet()) {
            halLinks.put(entry.getKey(), new HalLink(entry.getValue()));
        }
        return halLinks;
    }

    private ResourceLinksProvider restLinksProvider() {
        InstanceHandle<ResourceLinksProvider> instance = Arc.container().instance(ResourceLinksProvider.class);
        if (instance.isAvailable()) {
            return instance.get();
        }
        throw new IllegalStateException("No bean of type '" + ResourceLinksProvider.class.getName() + "' found.");
    }
}
