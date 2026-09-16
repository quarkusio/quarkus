package io.quarkus.platform.tools;

import static io.quarkus.registry.catalog.Extension.MD_CATEGORIES;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.ExtensionCatalog;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Warns about {@code metadata.categories} values in an extension descriptor that aren't part of a known set of
 * category ids.
 * <p>
 * The set of known category ids is supplied by the caller rather than bundled with this class: which categories are
 * valid is specific to the platform an extension targets, and that data shouldn't be baked into generic extension
 * tooling. Callers usually would get the information from the registry, but in a Quarkus build itself, there is a
 * chicken-and-egg bypass
 * to use the catalog information on disk instead. In that case it would be done via
 * {@link #resolveKnownCategories(Path)}, pointed at a
 * {@code catalog-overrides.json}-shaped file for the relevant platform.
 */
public final class ExtensionCategoryChecker {

    private final MessageWriter log;
    private final Set<String> knownCategories;

    public ExtensionCategoryChecker(Path catalogOverridesFile, MessageWriter log) throws IOException {
        this.log = log;
        knownCategories = resolveKnownCategories(catalogOverridesFile);
    }

    public ExtensionCategoryChecker(MessageWriter log) throws IOException {
        this(null, log);
    }

    /**
     * Resolves the known category ids to check an extension descriptor against: the {@code catalogOverridesFile} if
     * it exists and lists at least one category, otherwise the categories of the default platform's published
     * extension catalog, resolved the same way {@code mvn quarkus:list-categories} resolves it.
     * <p>
     * Any failure to resolve the fallback catalog (an offline build, no registries configured, the registry being
     * unreachable, etc.) results in an empty set rather than a failure, since this data is only ever used to produce
     * a non-blocking warning.
     *
     * @param catalogOverridesFile path to a {@code catalog-overrides.json}-shaped file, may be {@code null}
     */
    private Set<String> resolveKnownCategories(Path catalogOverridesFile) throws IOException {
        if (catalogOverridesFile != null) {
            final Set<String> local = loadKnownCategories(catalogOverridesFile);
            if (!local.isEmpty()) {
                return local;
            }
        }
        if (!QuarkusProjectHelper.isRegistryClientEnabled()) {
            return Set.of();
        }
        try {
            final ExtensionCatalogResolver catalogResolver = QuarkusProjectHelper.getCatalogResolver(log);
            if (!catalogResolver.hasRegistries()) {
                return Set.of();
            }
            final ExtensionCatalog catalog = catalogResolver.resolveExtensionCatalog();
            final Set<String> ids = new LinkedHashSet<>();
            for (Category category : catalog.getCategories()) {
                ids.add(category.getId());
            }
            return ids;
        } catch (Exception e) {
            log.debug("Could not resolve the known extension categories from the configured registries: %s",
                    e.getMessage());
            return Set.of();
        }
    }

    /**
     * Reads the {@code id} of every entry in the top-level {@code categories} array of a
     * {@code catalog-overrides.json}-shaped file.
     *
     * @param catalogOverridesFile path to the file, may be {@code null}
     * @return the known category ids, or an empty set if {@code catalogOverridesFile} is {@code null} or doesn't
     *         exist
     */
    private static Set<String> loadKnownCategories(Path catalogOverridesFile) throws IOException {
        if (catalogOverridesFile == null || !Files.isRegularFile(catalogOverridesFile)) {
            return Set.of();
        }
        final JsonNode root;
        try (InputStream is = Files.newInputStream(catalogOverridesFile)) {
            root = JsonMapper.builder().build().readTree(is);
        }
        final JsonNode categories = root.get(MD_CATEGORIES);
        if (categories == null || !categories.isArray()) {
            return Set.of();
        }
        final Set<String> ids = new LinkedHashSet<>();
        for (JsonNode category : categories) {
            final JsonNode id = category.get("id");
            if (id != null && id.isTextual()) {
                ids.add(id.asText());
            }
        }
        return Set.copyOf(ids);
    }

    /**
     * Returns a warning message for a single unknown category id, suitable for passing to a build tool's warn logger.
     * The message ends with a full stop; callers may append a tool-specific suggestion after it.
     */
    public static String warningFor(String unknownCategory) {
        return "Extension category '" + unknownCategory + "' is not part of the known Quarkus extension categories.";
    }

    /**
     * Returns the {@code metadata.categories} values of the given extension descriptor that aren't part of
     * {@code knownCategories}, in declaration order.
     * <p>
     * Returns an empty list if {@code knownCategories} is empty, since that means no category data was available to
     * check against, not that every category is unknown.
     */
    public List<String> findUnknownCategories(ObjectNode extObject) {
        if (knownCategories.isEmpty()) {
            return List.of();
        }
        final JsonNode metadata = extObject.get("metadata");
        if (metadata == null || !metadata.isObject()) {
            return List.of();
        }
        final JsonNode categories = metadata.get("categories");
        if (categories == null || !categories.isArray()) {
            return List.of();
        }
        final List<String> unknown = new ArrayList<>();
        for (JsonNode category : categories) {
            if (category.isTextual() && !knownCategories.contains(category.asText())) {
                unknown.add(category.asText());
            }
        }
        return unknown;
    }
}
