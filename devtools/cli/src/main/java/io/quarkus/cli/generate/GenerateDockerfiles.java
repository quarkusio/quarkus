package io.quarkus.cli.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(name = "dockerfiles", sortOptions = false, mixinStandardHelpOptions = false, header = "Generate Dockerfiles.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class GenerateDockerfiles extends AbstractGenerateCodestart {

    @Option(names = "--skip-dockerignore", description = "Flag to skip the generation of .dockerignore")
    boolean skipDockerIgnore;

    @Option(names = "--skip-flavor", arity = "0..n", description = "Flag to skip the generation of Dockerfile with flavor (e.g. jvm, native etc)")
    List<String> skipFlavors = new ArrayList<>();

    @Override
    public String getCodestart() {
        return "dockerfiles";
    }

    @Override
    public Map<String, String> getOutputStrategySpec() {
        Map<String, String> outputStrategySpec = new HashMap<>();
        outputStrategySpec.put("*", "replace");
        if (skipDockerIgnore) {
            outputStrategySpec.put(".dockerignore", "skip");
        }
        skipFlavors.forEach(f -> outputStrategySpec.put("*Dockerfile." + f, "skip"));
        return outputStrategySpec;
    }
}
