package io.quarkus.rest.data.panache.runtime.hal;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.rest.data.panache.runtime.resource.ResourceLinksProvider;

final class RestEasyHalLinksProvider implements HalLinksProvider {

    private final ResourceLinksProvider linksProvider = new ResourceLinksProvider();

    @Override
    public Map<String, HalLink> getLinks(Class<?> entityClass) {
        return toHalLinkMap(linksProvider.getClassLinks(entityClass));
    }

    @Override
    public Map<String, HalLink> getLinks(Object entity) {
        return toHalLinkMap(linksProvider.getInstanceLinks(entity));
    }

    private Map<String, HalLink> toHalLinkMap(Map<String, String> links) {
        Map<String, HalLink> halLinks = new HashMap<>(links.size());
        for (Map.Entry<String, String> entry : links.entrySet()) {
            halLinks.put(entry.getKey(), new HalLink(entry.getValue()));
        }
        return halLinks;
    }
}
