package io.quarkus.bootstrap.classloading;

import java.net.URL;

/**
 * A resource on the Class Path that has been loaded from a {@link ClassPathElement}
 */
public interface ClassPathResource {

    /**
     * @return The element that contains this resource
     */
    ClassPathElement getContainingElement();

    /**
     * @return The relative path that was used to load this resource from the {@link ClassPathElement}
     */
    String getPath();

    /**
     *
     * @return The URL of the resource
     */
    URL getUrl();

    /**
     * Loads the data contained in this resource and returns it as a byte array
     * 
     * @return The resource data
     */
    byte[] getData();

}
