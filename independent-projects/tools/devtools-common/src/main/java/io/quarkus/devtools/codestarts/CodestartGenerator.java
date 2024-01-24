package io.quarkus.devtools.codestarts;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.devtools.codestarts.core.CodestartProcessor;
import io.quarkus.devtools.codestarts.core.DefaultCodestartProjectDefinition;
import io.quarkus.devtools.codestarts.core.strategy.CodestartFileStrategy;
import io.quarkus.devtools.codestarts.core.strategy.CodestartFileStrategyHandler;
import io.quarkus.devtools.codestarts.core.strategy.ReplaceCodestartFileStrategyHandler;
import io.quarkus.devtools.codestarts.core.strategy.SkipCodestartFileStrategyHandler;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartProjectInput;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.SourceType;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class CodestartGenerator {

    private static final Map<String, CodestartFileStrategyHandler> TOOLING_STRATEGIES_BY_NAME = Stream
            .of(new ReplaceCodestartFileStrategyHandler(),
                    new SkipCodestartFileStrategyHandler())
            .collect(Collectors.toMap(CodestartFileStrategyHandler::name, Function.identity()));

    private final MessageWriter log;

    public CodestartGenerator(MessageWriter log) {
        this.log = log;
    }

    public void generate(String codestartName) throws Exception {
        Map<String, String> outputStrategySpec = new HashMap<>();
        outputStrategySpec.put("*", "replace");
        generate(codestartName, outputStrategySpec);
    }

    public void generate(String codestartName, Map<String, String> outputStrategySpec) throws Exception {
        Path projectPath = Paths.get(".").toAbsolutePath();
        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectPath);
        Map<String, Object> platformData = new HashMap<>();
        ExtensionCatalog mainCatalog = QuarkusProjectHelper.getCatalogResolver().resolveExtensionCatalog();
        if (mainCatalog.getMetadata().get("maven") != null) {
            platformData.put("maven", mainCatalog.getMetadata().get("maven"));
        }

        if (mainCatalog.getMetadata().get("gradle") != null) {
            platformData.put("gradle", mainCatalog.getMetadata().get("gradle"));
        }

        Collection<Codestart> codeStarts = QuarkusCodestartCatalog.fromBaseCodestartsResources().getCodestarts();
        SourceType sourceType = QuarkusProjectHelper.detectSourceType(projectPath);
        String javaVersion = JavaVersion.computeJavaVersion(sourceType, JavaVersion.DETECT_JAVA_RUNTIME_VERSION);

        platformData.put("java.version", javaVersion);
        CodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .noDockerfiles(false)
                .addCodestarts(codeStarts.stream().map(Codestart::getName).collect(Collectors.toList()))
                .buildTool(buildTool)
                .addData(platformData)
                .build();

        CodestartProjectDefinition projectDefinition = DefaultCodestartProjectDefinition.of(input, codeStarts);
        final Codestart projectCodestart = projectDefinition.getRequiredCodestart(CodestartType.TOOLING);

        final Map<String, Object> data = NestedMaps.deepMerge(Stream.of(
                projectDefinition.getSharedData(),
                projectDefinition.getDepsData(),
                projectDefinition.getCodestartProjectData()));

        final CodestartProcessor processor = new CodestartProcessor(
                MessageWriter.info(),
                sourceType.name(),
                projectPath,
                outputStrategies(outputStrategySpec),
                data);

        Optional<Codestart> codeStart = codeStarts.stream().filter(c -> codestartName.equals(c.getName())).findFirst();
        if (!codeStart.isPresent()) {
            log.error("No " + codestartName + " codestart found.");
            return;
        }
        projectCodestart.use(project -> {
            codeStart.ifPresent(c -> {
                processor.process(project, c);
                try {
                    processor.writeFiles();
                    log.info("Successfully generated " + codestartName + ".");
                } catch (IOException e) {
                    log.error("Failed to write " + codestartName + ".", e);
                }
            });
        });
    }

    public static List<CodestartFileStrategy> outputStrategies(Map<String, String> spec) {
        final List<CodestartFileStrategy> codestartFileStrategyHandlers = new ArrayList<>(spec.size());

        for (Map.Entry<String, String> entry : spec.entrySet()) {
            final CodestartFileStrategyHandler handler = TOOLING_STRATEGIES_BY_NAME.get(entry.getValue());
            if (handler == null) {
                throw new CodestartStructureException("CodestartFileStrategyHandler named '" + entry.getValue()
                        + "' not found. Used with filter '" + entry.getKey() + "'");
            }
            codestartFileStrategyHandlers.add(new CodestartFileStrategy(entry.getKey(), handler));
        }
        return codestartFileStrategyHandlers;
    }
}
