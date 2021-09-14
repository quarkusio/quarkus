package io.quarkus.devtools.codestarts.core;

import static io.quarkus.devtools.codestarts.core.CodestartCatalogs.findLanguageName;
import static io.quarkus.devtools.codestarts.core.CodestartCatalogs.findRequiredCodestart;
import static io.quarkus.devtools.codestarts.core.CodestartData.buildCodestartProjectData;
import static io.quarkus.devtools.codestarts.core.CodestartData.buildDependenciesData;
import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.CodestartProjectInput;
import io.quarkus.devtools.codestarts.CodestartType;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DefaultCodestartProjectDefinition implements CodestartProjectDefinition {

    private static final Comparator<Codestart> PROCESSING_ORDER = Comparator.comparingInt(Codestart::getTypeOrder)
            .thenComparing(Codestart::getName);

    private static final Comparator<Codestart> SHARED_DATA_MERGE_ORDER = PROCESSING_ORDER;

    private final String languageName;
    private final List<Codestart> codestarts;
    private final CodestartProjectInput projectInput;

    private DefaultCodestartProjectDefinition(CodestartProjectInput projectInput,
            String languageName,
            List<Codestart> codestarts) {
        this.projectInput = requireNonNull(projectInput, "codestartInput is required");
        this.languageName = requireNonNull(languageName, "languageName is required");
        this.codestarts = requireNonNull(codestarts, "codestarts is required");
    }

    public static CodestartProjectDefinition of(CodestartProjectInput projectInput,
            Collection<Codestart> codestarts) {
        final String languageName = findLanguageName(codestarts);
        findRequiredCodestart(codestarts, CodestartType.PROJECT);
        final List<Codestart> sorted = codestarts.stream()
                .sorted(PROCESSING_ORDER)
                .collect(Collectors.toList());
        return new DefaultCodestartProjectDefinition(projectInput, languageName, sorted);
    }

    @Override
    public void generate(Path targetDirectory) throws IOException {
        CodestartProjectGeneration.generateProject(this, targetDirectory);
    }

    @Override
    public List<Codestart> getCodestarts() {
        return codestarts;
    }

    @Override
    public CodestartProjectInput getProjectInput() {
        return projectInput;
    }

    @Override
    public Optional<Codestart> getCodestart(CodestartType type) {
        return CodestartCatalogs.findCodestart(codestarts, type);
    }

    @Override
    public Codestart getRequiredCodestart(CodestartType type) {
        return findRequiredCodestart(codestarts, type);
    }

    @Override
    public String getLanguageName() {
        return languageName;
    }

    @Override
    public Map<String, Object> getSharedData() {
        final Stream<Map<String, Object>> codestartsGlobal = getCodestarts().stream()
                .sorted(SHARED_DATA_MERGE_ORDER)
                .map(c -> c.getSharedData(getLanguageName()));
        return NestedMaps.deepMerge(Stream.concat(codestartsGlobal, Stream.of(getProjectInput().getData())));
    }

    @Override
    public Map<String, Object> getDepsData() {
        return buildDependenciesData(getCodestarts().stream(), getLanguageName(),
                getProjectInput().getDependencies(), getProjectInput().getBoms());
    }

    @Override
    public Map<String, Object> getCodestartProjectData() {
        return buildCodestartProjectData(getBaseCodestarts(), getExtraCodestarts());
    }

    @Override
    public List<Codestart> getBaseCodestarts() {
        return getCodestarts().stream().filter(c -> c.getSpec().getType().isBase()).collect(Collectors.toList());
    }

    @Override
    public List<Codestart> getExtraCodestarts() {
        return getCodestarts().stream().filter(c -> !c.getSpec().getType().isBase()).collect(Collectors.toList());
    }
}
