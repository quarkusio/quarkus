package io.quarkus.qute.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.qute.runtime.QuteConfig;

/**
 * Holds all template file paths, including the versions without suffixes configured via {@link QuteConfig#suffixes}.
 * <p>
 * For example, for the template {@code items.html} the set will contain {@code items.html} and {@code items}.
 */
public final class TemplateFilePathsBuildItem extends SimpleBuildItem {

    private final Set<String> filePaths;

    public TemplateFilePathsBuildItem(Set<String> filePaths) {
        this.filePaths = filePaths;
    }

    public Set<String> getFilePaths() {
        return filePaths;
    }

    public boolean contains(String path) {
        return filePaths.contains(path);
    }

}
