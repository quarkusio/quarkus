package io.quarkus.platform.catalog.processor;

import static io.quarkus.registry.catalog.Category.MD_PINNED;

import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ProcessedCategory {
    private final Category category;
    private final Collection<Extension> extensions;

    public ProcessedCategory(Category category, Collection<Extension> extensions) {
        this.category = Objects.requireNonNull(category);
        this.extensions = Objects.requireNonNull(extensions);
    }

    public Category getCategory() {
        return category;
    }

    public Collection<Extension> getExtensions() {
        return extensions;
    }

    public List<Extension> getSortedExtensions() {
        return extensions.stream().sorted(extensionsComparator()).collect(Collectors.toList());
    }

    private Comparator<Extension> extensionsComparator() {
        final List<String> pinnedList = MetadataValue.get(category.getMetadata(), MD_PINNED).asStringList();
        return Comparator.<Extension> comparingInt(e -> getPinnedIndex(pinnedList, e))
                .thenComparing(Extension::getName, String.CASE_INSENSITIVE_ORDER);
    }

    private int getPinnedIndex(List<String> pinnedList, Extension e) {
        final int index = pinnedList.indexOf(e.managementKey());
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

}
