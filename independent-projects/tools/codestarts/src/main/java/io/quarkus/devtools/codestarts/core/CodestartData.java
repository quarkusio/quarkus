package io.quarkus.devtools.codestarts.core;

import io.quarkus.devtools.codestarts.Codestart;
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

    private CodestartData() {
    }

    public static Optional<String> getBuildtool(final Map<String, Object> data) {
        return NestedMaps.getValue(data, "codestart-project.buildtool.name");
    }

    public static Map<String, Object> buildCodestartData(final Codestart codestart, final String languageName,
            final Map<String, Object> data) {
        final Optional<Map<String, Object>> value = NestedMaps.getValue(data, codestart.getName());
        Map<String, Object> codestartData = new HashMap<>();
        codestartData.putAll(data);
        NestedMaps.deepMerge(codestartData, codestart.getLocalData(languageName));
        value.ifPresent(map -> NestedMaps.deepMerge(codestartData, map));
        return codestartData;
    }

    public static Map<String, Object> buildCodestartProjectData(Collection<Codestart> baseCodestarts,
            Collection<Codestart> extraCodestarts) {
        final HashMap<String, Object> data = new HashMap<>();
        baseCodestarts.forEach((c) -> data.put("codestart-project." + c.getSpec().getType().toString().toLowerCase() + ".name",
                c.getName()));
        data.put("codestart-project.base-codestarts",
                baseCodestarts.stream().map(Codestart::getName).collect(Collectors.toList()));
        data.put("codestart-project.extra-codestarts",
                extraCodestarts.stream().map(Codestart::getName).collect(Collectors.toList()));
        return NestedMaps.unflatten(data);
    }

    public static Map<String, Object> buildDependenciesData(Stream<Codestart> codestartsStream, String languageName,
            Collection<String> extensions) {
        final Map<String, Set<CodestartDep>> depsData = new HashMap<>();
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
        depsData.put("test-dependencies", testDependencies);
        return Collections.unmodifiableMap(depsData);
    }

}
