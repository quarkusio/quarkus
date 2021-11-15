package io.quarkus.platform.catalog.processor;

import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonCategory;
import java.util.*;

public class CatalogProcessor {
    private static final String CODESTART_ARTIFACTS = "codestarts-artifacts";
    private static final String UNCATEGORIZED_ID = "uncategorized";
    private static final String UNCATEGORIZED_NAME = "Uncategorized";
    private static final String UNCATEGORIZED_DESCRIPTION = "The category is not defined for those extensions.";

    private final ExtensionCatalog catalog;

    private CatalogProcessor(ExtensionCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog);
    }

    public static CatalogProcessor of(ExtensionCatalog catalog) {
        return new CatalogProcessor(catalog);
    }

    public static List<ProcessedCategory> getProcessedCategoriesInOrder(ExtensionCatalog catalog) {
        final Map<String, List<Extension>> extsByCategory = new HashMap<>(catalog.getCategories().size());
        for (Extension e : catalog.getExtensions()) {
            List<String> categories = ExtensionProcessor.of(e).getCategories();
            if (categories.isEmpty()) {
                extsByCategory.put(UNCATEGORIZED_ID, new ArrayList<>());
                extsByCategory.get(UNCATEGORIZED_ID).add(e);
            }
            for (String c : categories) {
                if (!extsByCategory.containsKey(c)) {
                    extsByCategory.put(c, new ArrayList<>());
                }
                extsByCategory.get(c).add(e);
            }
        }
        final List<ProcessedCategory> orderedCategories = new ArrayList<>(catalog.getCategories().size());
        for (Category c : catalog.getCategories()) {
            if (extsByCategory.containsKey(c.getId())) {
                orderedCategories.add(new ProcessedCategory(c, extsByCategory.get(c.getId())));
            }
        }
        JsonCategory category = new JsonCategory();
        category.setId(UNCATEGORIZED_ID);
        category.setName(UNCATEGORIZED_NAME);
        category.setDescription(UNCATEGORIZED_DESCRIPTION);
        orderedCategories.add(new ProcessedCategory(category, extsByCategory.get(UNCATEGORIZED_ID)));
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
