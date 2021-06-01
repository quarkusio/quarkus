package io.quarkus.devtools.codestarts.core;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartType;
import io.quarkus.devtools.codestarts.core.CodestartSpec.CodestartDep;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CodestartData {

    public static final String INPUT_BASE_CODESTART_KEY_PREFIX = "input.base-codestart.";
    public static final String INPUT_BASE_CODESTARTS_KEY = "input.base-codestarts";
    public static final String INPUT_EXTRA_CODESTARTS_KEY = "input.extra-codestarts";

    private CodestartData() {
    }

    public static Optional<String> getInputCodestartForType(final Map<String, Object> data, final CodestartType type) {
        return NestedMaps.getValue(data, INPUT_BASE_CODESTART_KEY_PREFIX + type.toString().toLowerCase());
    }

    public static Optional<String> getBuildtool(final Map<String, Object> data) {
        return getInputCodestartForType(data, CodestartType.BUILDTOOL);
    }

    public static Map<String, Object> buildCodestartData(final Codestart codestart, final String languageName,
            final Map<String, Object> data) {
        final Optional<Map<String, Object>> value = NestedMaps.getValue(data, codestart.getName());
        Map<String, Object> withLocalCodestartData = NestedMaps.deepMerge(data, codestart.getLocalData(languageName));
        if (!value.isPresent()) {
            return withLocalCodestartData;
        }
        return NestedMaps.deepMerge(withLocalCodestartData, value.get());
    }

    public static Map<String, Object> buildCodestartProjectData(Collection<Codestart> baseCodestarts,
            Collection<Codestart> extraCodestarts) {
        final HashMap<String, Object> data = new HashMap<>();
        baseCodestarts.forEach((c) -> data.put(INPUT_BASE_CODESTART_KEY_PREFIX + c.getSpec().getType().toString().toLowerCase(),
                c.getName()));
        data.put(INPUT_BASE_CODESTARTS_KEY,
                baseCodestarts.stream().map(Codestart::getName).collect(Collectors.toList()));
        data.put(INPUT_EXTRA_CODESTARTS_KEY,
                extraCodestarts.stream().map(Codestart::getName).collect(Collectors.toList()));
        return NestedMaps.unflatten(data);
    }

    public static Map<String, Object> buildDependenciesData(Stream<Codestart> codestartsStream, String languageName,
            Collection<String> extensions, Collection<String> platforms) {
        final Map<String, Set<CodestartDep>> depsData = new HashMap<>();
        final Set<CodestartDep> boms = platforms.stream()
                .map(CodestartDep::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        final Set<CodestartDep> dependencies = extensions.stream()
                .map(CodestartDep::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        final Set<CodestartDep> testDependencies = new LinkedHashSet<>();
        codestartsStream
                .flatMap(s -> Stream.of(s.getBaseLanguageSpec(), s.getLanguageSpec(languageName)))
                .forEach(d -> {
                    dependencies.addAll(d.getDependencies());
                    testDependencies.addAll(d.getTestDependencies());
                });
        depsData.put("dependencies", dependencies);
        depsData.put("boms", boms);
        depsData.put("test-dependencies", testDependencies);
        return Collections.unmodifiableMap(depsData);
    }

}
