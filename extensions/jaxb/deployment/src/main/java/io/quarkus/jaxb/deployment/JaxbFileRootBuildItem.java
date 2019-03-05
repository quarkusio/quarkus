package io.quarkus.jaxb.deployment;

import org.jboss.builder.item.MultiBuildItem;

/**
 * A location that should be scanned for jaxb.index files
 */
public final class JaxbFileRootBuildItem extends MultiBuildItem {
    private final String fileRoot;

    public JaxbFileRootBuildItem(String fileRoot) {
        this.fileRoot = fileRoot;
    }

    public String getFileRoot() {
        return fileRoot;
    }
}
