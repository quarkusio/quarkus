package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * @deprecated This build item was previously used to enable JNI from Quarkus extensions,
 *             but JNI is always enabled starting from GraalVM 19.3.1.
 */
@Deprecated(since = "3.32", forRemoval = true)
public final class JniBuildItem extends MultiBuildItem {

    private final List<String> libraryPaths;

    @Deprecated(since = "3.32", forRemoval = true)
    public JniBuildItem() {
        this(Collections.emptyList());
    }

    @Deprecated(since = "3.32", forRemoval = true)
    public JniBuildItem(List<String> libraryPaths) {
        this.libraryPaths = libraryPaths;
    }

    @Deprecated(since = "3.32", forRemoval = true)
    public List<String> getLibraryPaths() {
        return libraryPaths;
    }

}
