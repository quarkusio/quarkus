package io.quarkus.resteasy.reactive.links;

import java.util.Collection;

import jakarta.ws.rs.core.Link;

/**
 * An injectable bean that contains methods to get the web links at class and instance levels.
 */
public interface RestLinksProvider {

    /**
     * @param elementType The resource type.
     * @return the web links associated with the element type.
     */
    Collection<Link> getTypeLinks(Class<?> elementType);

    /**
     * @param instance the resource instance.
     * @param <T> the resource generic type.
     * @return the web links associated with the instance.
     */
    <T> Collection<Link> getInstanceLinks(T instance);
}
