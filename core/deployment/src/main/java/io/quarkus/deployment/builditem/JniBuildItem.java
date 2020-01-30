package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class JniBuildItem extends MultiBuildItem {

    private final List<String> libraryPaths;

    /**
     * @deprecated This method was previously used to enable JNI from Quarkus extensions, but JNI is always enabled starting
     *             from GraalVM 19.3.1.
     */
    @Deprecated
    public JniBuildItem() {
        this(Collections.emptyList());
    }

    public JniBuildItem(List<String> libraryPaths) {
        this.libraryPaths = libraryPaths;
    }

    public List<String> getLibraryPaths() {
        return libraryPaths;
    }

}
