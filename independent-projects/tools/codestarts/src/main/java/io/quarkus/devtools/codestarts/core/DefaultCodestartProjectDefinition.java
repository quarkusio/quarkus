package io.quarkus.devtools.codestarts.core;

import static io.quarkus.devtools.codestarts.core.CodestartData.buildCodestartProjectData;
import static io.quarkus.devtools.codestarts.core.CodestartData.buildDependenciesData;
import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.CodestartProjectInput;
import io.quarkus.devtools.codestarts.CodestartResourceLoader;
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
    private final List<Codestart> implementedCodestarts;
    private final List<Codestart> unimplementedCodestarts;
    private final CodestartResourceLoader resourceLoader;
    private final CodestartProjectInput projectInput;

    private DefaultCodestartProjectDefinition(CodestartResourceLoader resourceLoader,
            CodestartProjectInput projectInput,
            String languageName,
            List<Codestart> implementedCodestarts,
            List<Codestart> unimplementedCodestarts) {
        this.resourceLoader = requireNonNull(resourceLoader, "resourceLoader is required");
        ;
        this.projectInput = requireNonNull(projectInput, "codestartInput is required");
        this.languageName = requireNonNull(languageName, "languageName is required");
        this.implementedCodestarts = requireNonNull(implementedCodestarts, "implementedCodestarts is required");
        this.unimplementedCodestarts = requireNonNull(unimplementedCodestarts, "unimplementedCodestarts is required");
    }

    public static CodestartProjectDefinition of(CodestartResourceLoader resourceLoader,
            CodestartProjectInput projectInput,
            Collection<Codestart> codestarts) {
        final String languageName = checkContainsType(codestarts, CodestartType.LANGUAGE).getName();
        final List<Codestart> implementedCodestarts = codestarts.stream()
                .filter(c -> c.implementsLanguage(languageName))
                .sorted(PROCESSING_ORDER)
                .collect(Collectors.toList());
        checkContainsType(implementedCodestarts, CodestartType.PROJECT);
        final List<Codestart> unimplemented = codestarts.stream()
                .filter(c -> !c.implementsLanguage(languageName))
                .collect(Collectors.toList());
        return new DefaultCodestartProjectDefinition(resourceLoader, projectInput, languageName, implementedCodestarts,
                unimplemented);
    }

    @Override
    public CodestartResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public void generate(Path targetDirectory) throws IOException {
        CodestartProjectGeneration.generateProject(this, targetDirectory);
    }

    @Override
    public List<Codestart> getImplementedCodestarts() {
        return implementedCodestarts;
    }

    @Override
    public List<Codestart> getUnimplementedCodestarts() {
        return unimplementedCodestarts;
    }

    @Override
    public CodestartProjectInput getProjectInput() {
        return projectInput;
    }

    @Override
    public Optional<Codestart> getCodestart(CodestartType type) {
        return getCodestart(implementedCodestarts, type);
    }

    @Override
    public Codestart getRequiredCodestart(CodestartType type) {
        return checkContainsType(implementedCodestarts, type);
    }

    @Override
    public String getLanguageName() {
        return languageName;
    }

    @Override
    public Map<String, Object> getSharedData() {
        final Stream<Map<String, Object>> codestartsGlobal = getImplementedCodestarts().stream()
                .sorted(SHARED_DATA_MERGE_ORDER)
                .map(c -> c.getSharedData(getLanguageName()));
        return NestedMaps.deepMerge(Stream.concat(codestartsGlobal, Stream.of(getProjectInput().getData())));
    }

    @Override
    public Map<String, Object> getDepsData() {
        return buildDependenciesData(getImplementedCodestarts().stream(), getLanguageName(),
                getProjectInput().getDependencies());
    }

    @Override
    public Map<String, Object> getCodestartProjectData() {
        return buildCodestartProjectData(getBaseCodestarts(), getExtraCodestarts());
    }

    @Override
    public List<Codestart> getBaseCodestarts() {
        return getImplementedCodestarts().stream().filter(c -> c.getSpec().getType().isBase()).collect(Collectors.toList());
    }

    @Override
    public List<Codestart> getExtraCodestarts() {
        return getImplementedCodestarts().stream().filter(c -> !c.getSpec().getType().isBase()).collect(Collectors.toList());
    }

    static Codestart checkContainsType(Collection<Codestart> codestarts, CodestartType type) {
        return getCodestart(codestarts, type)
                .orElseThrow(() -> new IllegalArgumentException(type.toString().toLowerCase() + " Codestart is required"));
    }

    static Optional<Codestart> getCodestart(Collection<Codestart> codestarts, CodestartType type) {
        return codestarts.stream().filter(c -> c.getType() == type).findFirst();
    }
}
