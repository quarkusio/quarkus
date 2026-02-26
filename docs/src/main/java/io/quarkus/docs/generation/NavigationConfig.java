package io.quarkus.docs.generation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Reads and validates the navigation configuration file ({@value #CONFIG_FILENAME}).
 * <p>
 * The configuration defines valid categories and subcategories, display metadata
 * (cat-title, use-case, subcat-title), featured guides, and validation limits.
 */
public class NavigationConfig {

    static final String CONFIG_FILENAME = "navigation-config.yaml";

    private int titleLimit = 40;
    private int featuredLimit = 4;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<CategoryEntry> categories = new ArrayList<>();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<FeaturedEntry> featured = new ArrayList<>();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<LearningPathEntry> learningPaths = new ArrayList<>();

    // Derived lookup structures (built after deserialization)
    private transient Map<String, CategoryEntry> categoryById;
    private transient Map<String, String> subcatTitleMap;
    private transient Map<String, Set<String>> subcatParentMap;
    private transient Map<String, String> featuredSummaryMap;
    private transient Set<String> featuredFileSet;

    /**
     * Load the navigation configuration from a YAML file.
     */
    public static NavigationConfig load(Path configFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        NavigationConfig config = mapper.readValue(configFile.toFile(), NavigationConfig.class);
        config.buildLookups();
        return config;
    }

    /**
     * Create a default configuration from the built-in Category enum values.
     * Used as a fallback when the configuration file does not exist.
     */
    static NavigationConfig createDefaultFromEnum() {
        NavigationConfig config = new NavigationConfig();
        for (YamlMetadataGenerator.Category cat : YamlMetadataGenerator.Category.values()) {
            CategoryEntry entry = new CategoryEntry();
            entry.category = cat.id;
            entry.catTitle = cat.name;
            config.categories.add(entry);
        }
        config.buildLookups();
        return config;
    }

    private void buildLookups() {
        categoryById = new LinkedHashMap<>();
        subcatTitleMap = new HashMap<>();
        subcatParentMap = new HashMap<>();
        featuredSummaryMap = new HashMap<>();
        featuredFileSet = new LinkedHashSet<>();

        for (CategoryEntry cat : categories) {
            categoryById.put(cat.category, cat);
            if (cat.subcategories != null) {
                for (SubcategoryEntry subcat : cat.subcategories) {
                    subcatParentMap
                            .computeIfAbsent(subcat.subcategory, k -> new LinkedHashSet<>())
                            .add(cat.category);
                    subcatTitleMap.put(subcat.subcategory, subcat.subcatTitle);
                }
            }
        }

        for (FeaturedEntry f : featured) {
            featuredFileSet.add(f.file);
            featuredSummaryMap.put(f.file, f.featuredSummary);
        }
    }

    /**
     * Validate the configuration for internal consistency.
     *
     * @return a list of error messages (empty if valid)
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (titleLimit <= 0) {
            errors.add("title-limit must be positive, got: " + titleLimit);
        }

        // Duplicate category IDs
        Set<String> seenCategories = new HashSet<>();
        for (CategoryEntry cat : categories) {
            if (cat.category == null || cat.category.isBlank()) {
                errors.add("Category entry missing 'category' ID.");
            } else if (!seenCategories.add(cat.category)) {
                errors.add("Duplicate category ID: '" + cat.category + "'.");
            }

            if (cat.catTitle == null || cat.catTitle.isBlank()) {
                errors.add("Category '" + cat.category + "' missing required 'cat-title'.");
            }

            if (cat.subcategories != null) {
                Set<String> seenSubcats = new HashSet<>();
                for (SubcategoryEntry subcat : cat.subcategories) {
                    if (subcat.subcategory == null || subcat.subcategory.isBlank()) {
                        errors.add("Subcategory entry under '" + cat.category + "' missing 'subcategory' ID.");
                    } else if (!seenSubcats.add(subcat.subcategory)) {
                        errors.add("Duplicate subcategory '" + subcat.subcategory
                                + "' under category '" + cat.category + "'.");
                    }

                    if (subcat.subcatTitle == null || subcat.subcatTitle.isBlank()) {
                        errors.add("Subcategory '" + subcat.subcategory + "' under '"
                                + cat.category + "' missing required 'subcat-title'.");
                    }
                }
            }
        }

        // Subcategory title consistency across categories
        Map<String, String> firstTitle = new HashMap<>();
        for (CategoryEntry cat : categories) {
            if (cat.subcategories != null) {
                for (SubcategoryEntry subcat : cat.subcategories) {
                    if (subcat.subcategory == null) {
                        continue;
                    }
                    String existing = firstTitle.putIfAbsent(subcat.subcategory, subcat.subcatTitle);
                    if (existing != null && !existing.equals(subcat.subcatTitle)) {
                        errors.add("Subcategory '" + subcat.subcategory
                                + "' has inconsistent subcat-title: '" + existing
                                + "' vs '" + subcat.subcatTitle + "'.");
                    }
                }
            }
        }

        // Featured limit
        if (featured.size() > featuredLimit) {
            errors.add("Featured list has " + featured.size()
                    + " entries but featured-limit is " + featuredLimit + ".");
        }

        for (FeaturedEntry f : featured) {
            if (f.file == null || f.file.isBlank()) {
                errors.add("Featured entry missing 'file'.");
            }
            if (f.featuredSummary == null || f.featuredSummary.isBlank()) {
                errors.add("Featured entry for '" + f.file + "' missing 'featured-summary'.");
            }
        }

        // Learning paths
        Set<String> seenPaths = new HashSet<>();
        for (LearningPathEntry lp : learningPaths) {
            if (lp.path == null || lp.path.isBlank()) {
                errors.add("Learning path entry missing 'path' ID.");
            } else if (!seenPaths.add(lp.path)) {
                errors.add("Duplicate learning path ID: '" + lp.path + "'.");
            }

            if (lp.pathTitle == null || lp.pathTitle.isBlank()) {
                errors.add("Learning path '" + lp.path + "' missing required 'path-title'.");
            }

            if (lp.guides == null || lp.guides.isEmpty()) {
                errors.add("Learning path '" + lp.path + "' has no guides listed.");
            } else {
                Set<String> seenGuides = new HashSet<>();
                for (String guide : lp.guides) {
                    if (!seenGuides.add(guide)) {
                        errors.add("Duplicate guide '" + guide + "' in learning path '" + lp.path + "'.");
                    }
                }
            }
        }

        return errors;
    }

    // --- Lookup methods ---

    public boolean isValidCategory(String id) {
        return categoryById != null && categoryById.containsKey(id);
    }

    public boolean isValidSubcategory(String id) {
        return subcatTitleMap != null && subcatTitleMap.containsKey(id);
    }

    public String getCatTitle(String categoryId) {
        CategoryEntry entry = categoryById != null ? categoryById.get(categoryId) : null;
        return entry != null ? entry.catTitle : null;
    }

    public String getUseCase(String categoryId) {
        CategoryEntry entry = categoryById != null ? categoryById.get(categoryId) : null;
        return entry != null ? entry.useCase : null;
    }

    public String getSubcatTitle(String subcategoryId) {
        return subcatTitleMap != null ? subcatTitleMap.get(subcategoryId) : null;
    }

    public Set<String> getParentCategories(String subcategoryId) {
        return subcatParentMap != null
                ? subcatParentMap.getOrDefault(subcategoryId, Set.of())
                : Set.of();
    }

    public int getTitleLimit() {
        return titleLimit;
    }

    public int getFeaturedLimit() {
        return featuredLimit;
    }

    public List<CategoryEntry> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public List<FeaturedEntry> getFeatured() {
        return Collections.unmodifiableList(featured);
    }

    public List<LearningPathEntry> getLearningPaths() {
        return Collections.unmodifiableList(learningPaths);
    }

    public boolean isFeatured(String filename) {
        return featuredFileSet != null && featuredFileSet.contains(filename);
    }

    public String getFeaturedSummary(String filename) {
        return featuredSummaryMap != null ? featuredSummaryMap.get(filename) : null;
    }

    /**
     * Get all valid category IDs in configuration order.
     */
    public Set<String> getCategoryIds() {
        return categoryById != null ? Collections.unmodifiableSet(categoryById.keySet()) : Set.of();
    }

    /**
     * Get subcategory entries for a category, in configuration order.
     */
    public List<SubcategoryEntry> getSubcategories(String categoryId) {
        CategoryEntry entry = categoryById != null ? categoryById.get(categoryId) : null;
        if (entry == null || entry.subcategories == null) {
            return List.of();
        }
        return Collections.unmodifiableList(entry.subcategories);
    }

    // --- Inner data classes for YAML deserialization ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryEntry {
        private String category;
        private String catTitle;
        private String useCase;

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        private List<SubcategoryEntry> subcategories = new ArrayList<>();

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getCatTitle() {
            return catTitle;
        }

        public void setCatTitle(String catTitle) {
            this.catTitle = catTitle;
        }

        public String getUseCase() {
            return useCase;
        }

        public void setUseCase(String useCase) {
            this.useCase = useCase;
        }

        public List<SubcategoryEntry> getSubcategories() {
            return subcategories;
        }

        public void setSubcategories(List<SubcategoryEntry> subcategories) {
            this.subcategories = subcategories;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubcategoryEntry {
        private String subcategory;
        private String subcatTitle;

        public String getSubcategory() {
            return subcategory;
        }

        public void setSubcategory(String subcategory) {
            this.subcategory = subcategory;
        }

        public String getSubcatTitle() {
            return subcatTitle;
        }

        public void setSubcatTitle(String subcatTitle) {
            this.subcatTitle = subcatTitle;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeaturedEntry {
        private String file;
        private String featuredSummary;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getFeaturedSummary() {
            return featuredSummary;
        }

        public void setFeaturedSummary(String featuredSummary) {
            this.featuredSummary = featuredSummary;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LearningPathEntry {
        private String path;
        private String pathTitle;
        private String pathSummary;

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        private List<String> guides = new ArrayList<>();

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPathTitle() {
            return pathTitle;
        }

        public void setPathTitle(String pathTitle) {
            this.pathTitle = pathTitle;
        }

        public String getPathSummary() {
            return pathSummary;
        }

        public void setPathSummary(String pathSummary) {
            this.pathSummary = pathSummary;
        }

        public List<String> getGuides() {
            return guides;
        }

        public void setGuides(List<String> guides) {
            this.guides = guides;
        }
    }

    // --- Root-level setters for Jackson deserialization ---

    public void setTitleLimit(int titleLimit) {
        this.titleLimit = titleLimit;
    }

    public void setFeaturedLimit(int featuredLimit) {
        this.featuredLimit = featuredLimit;
    }

    public void setCategories(List<CategoryEntry> categories) {
        this.categories = categories;
    }

    public void setFeatured(List<FeaturedEntry> featured) {
        this.featured = featured;
    }

    public void setLearningPaths(List<LearningPathEntry> learningPaths) {
        this.learningPaths = learningPaths;
    }
}
