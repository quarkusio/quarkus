package io.quarkus.smallrye.openapi.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;

/**
 * Internal build item of the smallrye-openapi extension, providing the resolved filterNames combined with their
 * {@link io.quarkus.smallrye.openapi.OpenApiFilter.RunStage} and documentName information.
 * Only produced by
 * {@link SmallRyeOpenApiFiltersProcessor#produceFilters(SmallRyeOpenApiConfig, OpenApiFilteredIndexViewBuildItem)}
 */
public final class DocumentFiltersBuildItem extends SimpleBuildItem {

    private final Map<String, Map<OpenApiFilter.RunStage, List<String>>> filterNames;

    private DocumentFiltersBuildItem(Builder builder) {
        this.filterNames = builder.filterNames;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<OpenApiFilter.RunStage, List<String>> filterNamesFor(String documentName) {
        Map<OpenApiFilter.RunStage, List<String>> filterNamesByStage = filterNames.get(documentName);
        if (filterNamesByStage == null) {
            return Collections.emptyMap();
        }

        Map<OpenApiFilter.RunStage, List<String>> collected = new EnumMap<>(OpenApiFilter.RunStage.class);
        filterNamesByStage.forEach((key, value) -> collected.put(key, Collections.unmodifiableList(value)));

        return collected;
    }

    public List<String> filterNamesFor(String documentName, OpenApiFilter.RunStage stage) {
        Map<OpenApiFilter.RunStage, List<String>> filterNamesByStage = filterNames.get(documentName);
        if (filterNamesByStage == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(filterNamesByStage.get(stage));
    }

    public Set<String> allFilterNamesFor(OpenApiFilter.RunStage... stages) {
        if (stages == null || stages.length == 0) {
            return Collections.emptySet();
        }

        Set<String> collected = new LinkedHashSet<>();
        for (Map<OpenApiFilter.RunStage, List<String>> filterNamesByStage : filterNames.values()) {
            for (OpenApiFilter.RunStage stage : stages) {
                collected.addAll(filterNamesByStage.get(stage));
            }
        }

        return Collections.unmodifiableSet(collected);
    }

    public static final class Builder {

        private final Map<String, Map<OpenApiFilter.RunStage, List<String>>> filterNames = new HashMap<>();

        private Builder() {
        }

        /**
         * Initializes this builder with all the known document names from config. Also ensure that every document contains
         * every RunStage, so that no caller has to handle null.
         * Must be called before {@link #addFilterName(String, OpenApiFilter.RunStage, String)}
         */
        public Builder addDocuments(SmallRyeOpenApiConfig config) {
            for (String documentName : config.documents().keySet()) {
                Map<OpenApiFilter.RunStage, List<String>> stages = new EnumMap<>(OpenApiFilter.RunStage.class);
                for (OpenApiFilter.RunStage stage : OpenApiFilter.RunStage.values()) {
                    stages.put(stage, new ArrayList<>());
                }

                filterNames.put(documentName, stages);
            }

            return this;
        }

        /**
         * Add the given filtername. Will no-op when the documentName is not already known. See
         * {@link #addDocuments(SmallRyeOpenApiConfig)}
         */
        public Builder addFilterName(String documentName, OpenApiFilter.RunStage stage, String filterName) {

            if (!filterNames.containsKey(documentName)) {
                return this;
            }

            filterNames.get(documentName).get(stage).add(filterName);

            return this;
        }

        public DocumentFiltersBuildItem build() {
            return new DocumentFiltersBuildItem(this);
        }
    }
}
