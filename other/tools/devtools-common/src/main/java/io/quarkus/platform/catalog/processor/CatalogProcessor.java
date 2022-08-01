package io.quarkus.platform.catalog.processor;

import static io.quarkus.platform.catalog.processor.ExtensionProcessor.isUnlisted;

import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.util.*;

public class CatalogProcessor {
    private static final String CODESTART_ARTIFACTS = "codestarts-artifacts";
    private static final Category UNCATEGORIZED_CATEGORY;

    private final ExtensionCatalog catalog;

    static {
        Category.Mutable draft = Category.builder()
                .setId("uncategorized")
                .setName("Uncategorized")
                .setDescription("The category is not defined for those extensions.");
        UNCATEGORIZED_CATEGORY = draft.build();
    }

    private CatalogProcessor(ExtensionCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog);
    }

    public static CatalogProcessor of(ExtensionCatalog catalog) {
        return new CatalogProcessor(catalog);
    }

    public static List<ProcessedCategory> getProcessedCategoriesInOrder(ExtensionCatalog catalog) {
        final Map<String, List<Extension>> extsByCategory = new HashMap<>(catalog.getCategories().size());
        for (Extension e : catalog.getExtensions()) {
            List<String> categories = new ArrayList<>(ExtensionProcessor.of(e).getCategories());
            if (!isUnlisted(e)) {
                if (categories.isEmpty()) {
                    categories.add(UNCATEGORIZED_CATEGORY.getId());
                }
                for (String c : categories) {
                    if (!extsByCategory.containsKey(c)) {
                        extsByCategory.put(c, new ArrayList<>());
                    }
                    extsByCategory.get(c).add(e);
                }
            }
        }

        final List<ProcessedCategory> orderedCategories = new ArrayList<>(catalog.getCategories().size());
        final List<Category> categories = new ArrayList<>(catalog.getCategories());
        if (categories.stream().noneMatch(c -> Objects.equals(c.getId(), UNCATEGORIZED_CATEGORY.getId()))) {
            categories.add(UNCATEGORIZED_CATEGORY);
        }
        for (Category c : categories) {
            if (extsByCategory.containsKey(c.getId())) {
                orderedCategories.add(new ProcessedCategory(c, extsByCategory.get(c.getId())));
            }
        }

        return orderedCategories;
    }

    public static List<String> getCodestartArtifacts(ExtensionCatalog catalog) {
        return getMetadataValue(catalog, CODESTART_ARTIFACTS).asStringList();
    }

    public ExtensionCatalog getCatalog() {
        return catalog;
    }

    public List<String> getCodestartArtifacts() {
        return getCodestartArtifacts(catalog);
    }

    public List<ProcessedCategory> getProcessedCategoriesInOrder() {
        return getProcessedCategoriesInOrder(catalog);
    }

    public static MetadataValue getMetadataValue(ExtensionCatalog catalog, String path) {
        return MetadataValue.get(catalog.getMetadata(), path);
    }
}
