package io.quarkus.qute.deployment;

import java.util.List;
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
    private final List<String> suffixes;

    public TemplateFilePathsBuildItem(Set<String> filePaths) {
        this(filePaths, List.of());
    }

    public TemplateFilePathsBuildItem(Set<String> filePaths, List<String> suffixes) {
        this.filePaths = filePaths;
        this.suffixes = suffixes;
    }

    public Set<String> getFilePaths() {
        return filePaths;
    }

    /**
     * @return the suffixes configured via {@link QuteConfig#suffixes} that were used to compute the paths without suffix
     */
    public List<String> getSuffixes() {
        return suffixes;
    }

    public boolean contains(String path) {
        return filePaths.contains(path);
    }

}
