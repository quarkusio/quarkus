package io.quarkus.rest.spi;

import java.util.Set;

public interface NameBoundBuildItem {

    /**
     * Returns the name binding names for this build item, or an empty
     * set if there are none.
     */
    Set<String> getNameBindingNames();

}
