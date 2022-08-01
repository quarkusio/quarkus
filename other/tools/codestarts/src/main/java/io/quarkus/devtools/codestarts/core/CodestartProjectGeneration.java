package io.quarkus.devtools.codestarts.core;

import static io.quarkus.devtools.codestarts.core.CodestartProcessor.buildStrategies;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.CodestartType;
import io.quarkus.devtools.codestarts.core.strategy.CodestartFileStrategy;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import io.quarkus.devtools.messagewriter.MessageWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class CodestartProjectGeneration {

    private CodestartProjectGeneration() {
    }

    static void generateProject(final CodestartProjectDefinition projectDefinition, final Path targetDirectory)
            throws IOException {
        final MessageWriter log = projectDefinition.getProjectInput().log();

        final String languageName = projectDefinition.getLanguageName();

        // Processing data
        final Map<String, Object> data = NestedMaps.deepMerge(Stream.of(
                projectDefinition.getSharedData(),
                projectDefinition.getDepsData(),
                projectDefinition.getCodestartProjectData()));

        log.debug("processed shared-data: %s" + data);

        final Codestart projectCodestart = projectDefinition.getRequiredCodestart(CodestartType.PROJECT);

        final List<CodestartFileStrategy> strategies = buildStrategies(mergeStrategies(projectDefinition));

        log.debug("file strategies: %s", strategies);
        CodestartProcessor processor = new CodestartProcessor(log,
                languageName, targetDirectory, strategies, data);
        processor.checkTargetDir();

        projectCodestart.use(project -> {
            for (Codestart codestart : projectDefinition.getCodestarts()) {
                processor.process(project, codestart);
            }
        });

        processor.writeFiles();

        log.info("\napplying codestarts...");
        log.info(projectDefinition.getCodestarts().stream()
                .map(c -> c.getType().getIcon() + " "
                        + c.getName())
                .collect(Collectors.joining(System.lineSeparator())));
    }

    private static Map<String, String> mergeStrategies(CodestartProjectDefinition projectDefinition) {
        return NestedMaps.deepMerge(
                projectDefinition.getCodestarts().stream().map(Codestart::getSpec)
                        .map(CodestartSpec::getOutputStrategy));
    }

}
