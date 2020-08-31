package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.CodestartData.buildCodestartProjectData;
import static io.quarkus.devtools.codestarts.CodestartData.buildDependenciesData;

import io.quarkus.devtools.codestarts.CodestartSpec.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CodestartProject {

    private final List<Codestart> codestarts;
    private final CodestartInput codestartInput;

    CodestartProject(CodestartInput codestartInput, List<Codestart> codestarts) {
        this.codestartInput = Objects.requireNonNull(codestartInput, "codestartInput is required");
        this.codestarts = Objects.requireNonNull(codestarts, "codestarts is required");
        checkContainsType(Type.PROJECT);
        checkContainsType(Type.LANGUAGE);
    }

    public List<Codestart> getCodestarts() {
        return codestarts;
    }

    public CodestartInput getCodestartInput() {
        return codestartInput;
    }

    public Optional<Codestart> getCodestart(Type type) {
        return codestarts.stream().filter(c -> c.getType() == type).findFirst();
    }

    public Codestart getRequiredCodestart(Type type) {
        return checkContainsType(type);
    }

    public String getLanguageName() {
        return getRequiredCodestart(Type.LANGUAGE).getName();
    }

    public Map<String, Object> getSharedData() {
        final Stream<Map<String, Object>> codestartsGlobal = getCodestarts().stream()
                .map(c -> c.getSharedData(getLanguageName()));
        return NestedMaps.deepMerge(Stream.concat(codestartsGlobal, Stream.of(getCodestartInput().getData())));
    }

    public Map<String, Object> getDepsData() {
        return buildDependenciesData(getCodestarts().stream(), getLanguageName(), getCodestartInput().getDependencies());
    }

    public Map<String, Object> getCodestartProjectData() {
        return buildCodestartProjectData(getBaseCodestarts(), getExtraCodestarts());
    }

    List<Codestart> getBaseCodestarts() {
        return getCodestarts().stream().filter(c -> c.getSpec().getType().isBase()).collect(Collectors.toList());
    }

    List<Codestart> getExtraCodestarts() {
        return getCodestarts().stream().filter(c -> !c.getSpec().getType().isBase()).collect(Collectors.toList());
    }

    private Codestart checkContainsType(Type type) {
        return getCodestart(type)
                .orElseThrow(() -> new IllegalArgumentException(type.toString().toLowerCase() + " Codestart is required"));
    }
}
