package io.quarkus.platform.catalog.processor;

import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.util.*;

public class CatalogProcessor {
    private final ExtensionCatalog catalog;

    private CatalogProcessor(ExtensionCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog);
    }

    public static CatalogProcessor of(ExtensionCatalog catalog) {
        return new CatalogProcessor(catalog);
    }

    public static List<ProcessedCategory> getProcessedCategoriesInOrder(ExtensionCatalog catalog) {
        return of(catalog).getProcessedCategoriesInOrder();
    }

    public ExtensionCatalog getCatalog() {
        return catalog;
    }

    public List<ProcessedCategory> getProcessedCategoriesInOrder() {
        final Map<String, List<Extension>> extsByCategory = new HashMap<>(getCatalog().getCategories().size());
        for (Extension e : catalog.getExtensions()) {
            List<String> categories = ExtensionProcessor.of(e).getCategories();
            for (String c : categories) {
                if (!extsByCategory.containsKey(c)) {
                    extsByCategory.put(c, new ArrayList<>());
                }
                extsByCategory.get(c).add(e);
            }
        }
        final List<ProcessedCategory> orderedCategories = new ArrayList<>(getCatalog().getCategories().size());
        for (Category c : getCatalog().getCategories()) {
            if (extsByCategory.containsKey(c.getId())) {
                orderedCategories.add(new ProcessedCategory(c, extsByCategory.get(c.getId())));
            }
        }
        return orderedCategories;
    }
}
