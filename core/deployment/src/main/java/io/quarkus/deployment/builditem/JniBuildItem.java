package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class JniBuildItem extends MultiBuildItem {

    private final List<String> libraryPaths;

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
