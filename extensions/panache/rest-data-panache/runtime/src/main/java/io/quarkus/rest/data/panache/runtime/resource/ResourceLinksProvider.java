package io.quarkus.rest.data.panache.runtime.resource;

import java.util.Map;

public interface ResourceLinksProvider {

    Map<String, String> getClassLinks(Class<?> className);

    Map<String, String> getInstanceLinks(Object instance);

    @SuppressWarnings("unused")
    String getSelfLink(Object instance);
}
