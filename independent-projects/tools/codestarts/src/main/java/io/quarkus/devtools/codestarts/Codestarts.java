package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.CodestartData.DataKey.BUILDTOOL;
import static io.quarkus.devtools.codestarts.CodestartLoader.loadAllCodestarts;
import static io.quarkus.devtools.codestarts.CodestartProcessor.buildStrategies;
import static io.quarkus.devtools.codestarts.CodestartSpec.Type.LANGUAGE;

import io.quarkus.devtools.codestarts.strategy.CodestartFileStrategy;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Codestarts {

    public static CodestartProject prepareProject(final CodestartInput input) throws IOException {
        final Optional<String> buildtool = NestedMaps.getValue(input.getData(), BUILDTOOL.getKey());
        final Set<String> selectedCodestartNames = Stream.concat(
                input.getCodestarts().stream(),
                Stream.of(buildtool.orElse(null)).filter(Objects::nonNull))
                .collect(Collectors.toSet());

        final List<Codestart> allCodestarts = loadAllCodestarts(input);

        final Collection<Codestart> baseCodestarts = resolveSelectedBaseCodestarts(allCodestarts, selectedCodestartNames);
        final String languageName = baseCodestarts.stream().filter(c -> c.getType() == LANGUAGE).findFirst()
                .orElseThrow(() -> new CodestartDefinitionException("Language codestart is required")).getName();
        final Collection<Codestart> extraCodestarts = resolveSelectedExtraCodestarts(input, selectedCodestartNames,
                allCodestarts, languageName);

        final List<Codestart> selectedCodestarts = new ArrayList<>();
        selectedCodestarts.addAll(baseCodestarts);
        selectedCodestarts.addAll(extraCodestarts);

        // include fallback example codestarts if none selected
        if (input.includeExamples()
                && selectedCodestarts.stream().noneMatch(c -> c.getSpec().isExample() && !c.getSpec().isPreselected())) {
            final List<Codestart> fallbackExampleCodestarts = resolveFallbackExampleCodestarts(allCodestarts,
                    languageName);
            selectedCodestarts.addAll(fallbackExampleCodestarts);
        }

        return new CodestartProject(input, selectedCodestarts);
    }

    public static void generateProject(final CodestartProject codestartProject, final Path targetDirectory) throws IOException {

        final String languageName = codestartProject.getLanguageName();

        // Processing data
        final Map<String, Object> data = NestedMaps.deepMerge(Stream.of(
                codestartProject.getSharedData(),
                codestartProject.getDepsData(),
                codestartProject.getCodestartProjectData()));

        final Codestart projectCodestart = codestartProject.getRequiredCodestart(CodestartSpec.Type.PROJECT);
        final List<CodestartFileStrategy> strategies = buildStrategies(mergeStrategies(codestartProject));

        CodestartProcessor processor = new CodestartProcessor(codestartProject.getCodestartInput().getResourceLoader(),
                languageName, targetDirectory, strategies, data);
        processor.checkTargetDir();
        for (Codestart codestart : codestartProject.getCodestarts()) {
            processor.process(codestart);
        }
        processor.writeFiles();
    }

    private static Map<String, String> mergeStrategies(CodestartProject codestartProject) {
        return NestedMaps.deepMerge(
                codestartProject.getCodestarts().stream().map(Codestart::getSpec).map(CodestartSpec::getOutputStrategy));
    }

    private static Collection<Codestart> resolveSelectedExtraCodestarts(CodestartInput input,
            Set<String> selectedCodestartNames,
            Collection<Codestart> allCodestarts,
            String languageName) {
        return allCodestarts.stream()
                .filter(c -> !c.getSpec().getType().isBase())
                .filter(c -> c.getSpec().isPreselected() || selectedCodestartNames.contains(c.getSpec().getRef()))
                .filter(c -> !c.getSpec().isExample() || input.includeExamples())
                .filter(c -> c.implementsLanguage(languageName))
                .collect(Collectors.toList());
    }

    private static Collection<Codestart> resolveSelectedBaseCodestarts(Collection<Codestart> allCodestarts,
            Set<String> selectedCodestartNames) {
        return allCodestarts.stream()
                .filter(c -> c.getSpec().getType().isBase())
                .filter(c -> c.getSpec().isFallback() || selectedCodestartNames.contains(c.getSpec().getRef()))
                .collect(Collectors.toMap(c -> c.getSpec().getType(), c -> c, (a, b) -> {
                    // When there is multiple matches for one key, one should be selected and the other a fallback.
                    if (a.getSpec().isFallback() && b.getSpec().isFallback()) {
                        throw new CodestartDefinitionException(
                                "Multiple fallback found for a base codestart of type: '" + a.getSpec().getType()
                                        + "' that should be unique. Only one of '" + a.getSpec().getName() + "' and '"
                                        + b.getSpec().getName() + "' should be a fallback");
                    }
                    if (!a.getSpec().isFallback() && !b.getSpec().isFallback()) {
                        throw new CodestartException(
                                "Multiple selection for base codestart of type: '" + a.getSpec().getType()
                                        + "' that should be unique. Only one of '" + a.getSpec().getName() + "' and '"
                                        + b.getSpec().getName() + "' should be selected at once.");
                    }
                    // The selected is picked.
                    return !a.getSpec().isFallback() ? a : b;
                })).values();
    }

    private static List<Codestart> resolveFallbackExampleCodestarts(List<Codestart> allCodestarts,
            String languageName) {
        return allCodestarts.stream()
                .filter(c -> !c.getSpec().getType().isBase())
                .filter(c -> c.getSpec().isExample() && c.getSpec().isFallback())
                .filter(c -> c.implementsLanguage(languageName))
                .collect(Collectors.toList());
    }

}
