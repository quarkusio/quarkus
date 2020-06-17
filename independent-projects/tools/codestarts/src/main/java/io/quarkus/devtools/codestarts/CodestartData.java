package io.quarkus.devtools.codestarts;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.codestarts.CodestartSpec.CodestartDep;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CodestartData {

    private CodestartData() {
    }

    public enum DataKey {
        BOM_GROUP_ID("quarkus.platform.group-id"),
        BOM_ARTIFACT_ID("quarkus.platform.artifact-id"),
        BOM_VERSION("quarkus.platform.version"),
        PROJECT_GROUP_ID("project.group-id"),
        PROJECT_ARTIFACT_ID("project.artifact-id"),
        PROJECT_VERSION("project.version"),
        QUARKUS_PLUGIN_GROUP_ID("quarkus.plugin.group-id"),
        QUARKUS_PLUGIN_ARTIFACT_ID("quarkus.plugin.artifact-id"),
        QUARKUS_PLUGIN_VERSION("quarkus.plugin.version"),
        QUARKUS_VERSION("quarkus.version"),
        BUILDTOOL("buildtool.name"),
        JAVA_VERSION("java.version");

        private final String key;

        DataKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public enum LegacySupport {
        BOM_GROUP_ID("bom_groupId"),
        BOM_ARTIFACT_ID("bom_artifactId"),
        BOM_VERSION("bom_version"),
        PROJECT_GROUP_ID("project_groupId"),
        PROJECT_ARTIFACT_ID("project_artifactId"),
        PROJECT_VERSION("project_version"),
        QUARKUS_PLUGIN_GROUP_ID("plugin_groupId"),
        QUARKUS_PLUGIN_ARTIFACT_ID("plugin_artifactId"),
        QUARKUS_PLUGIN_VERSION("plugin_version"),
        QUARKUS_VERSION("quarkus_version"),
        JAVA_VERSION("java_target");

        private final String legacyKey;
        private final String key;

        LegacySupport(String legacyKey) {
            this.key = DataKey.valueOf(this.name()).getKey();
            this.legacyKey = legacyKey;
        }

        public String getKey() {
            return key;
        }

        public String getLegacyKey() {
            return legacyKey;
        }

        public static Map<String, Object> convertFromLegacy(Map<String, Object> legacy) {
            return NestedMaps.unflatten(Stream.of(values())
                    .filter(v -> v.getLegacyKey() != null)
                    .filter(v -> legacy.containsKey(v.getLegacyKey()))
                    .map(v -> new HashMap.SimpleImmutableEntry<>(v.getKey(), legacy.get(v.getLegacyKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
    }

    static Map<String, Object> buildCodestartData(final Codestart codestart, final String languageName,
            final Map<String, Object> data) {
        return NestedMaps.deepMerge(Stream.of(codestart.getLocalData(languageName), data));
    }

    public static Map<String, Object> buildCodestartProjectData(Collection<Codestart> codestarts) {
        final HashMap<String, Object> data = new HashMap<>();
        codestarts.forEach((c) -> data.put("codestart-project." + c.getSpec().getType().toString().toLowerCase() + ".name",
                c.getSpec().getName()));
        return NestedMaps.unflatten(data);
    }

    static Map<String, Object> buildDependenciesData(Stream<Codestart> codestartsStream, String languageName,
            Collection<AppArtifactKey> extensions) {
        final Map<String, Set<CodestartDep>> depsData = new HashMap<>();
        final Set<CodestartDep> extensionsAsDeps = extensions.stream()
                .map(k -> k.getGroupId() + ":" + k.getArtifactId()).map(CodestartDep::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        final Set<CodestartDep> dependencies = new LinkedHashSet<>(extensionsAsDeps);
        final Set<CodestartDep> testDependencies = new LinkedHashSet<>();
        codestartsStream
                .flatMap(s -> Stream.of(s.getBaseLanguageSpec(), s.getLanguageSpec(languageName)))
                .forEach(d -> {
                    dependencies.addAll(d.getDependencies());
                    testDependencies.addAll(d.getTestDependencies());
                });
        depsData.put("dependencies", dependencies);
        depsData.put("test-dependencies", testDependencies);
        return Collections.unmodifiableMap(depsData);
    }

}
