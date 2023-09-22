package io.quarkus.cli.generate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.registry.ToggleRegistryClientMixin;
import io.quarkus.devtools.codestarts.CodestartGenerator;
import picocli.CommandLine;

public abstract class AbstractGenerateCodestart implements Callable<Integer> {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected ToggleRegistryClientMixin registryClient;

    public abstract String getCodestart();

    public Map<String, String> getOutputStrategySpec() {
        Map<String, String> outputStrategySpec = new HashMap<>();
        outputStrategySpec.put("*", "replace");
        return outputStrategySpec;
    }

    @Override
    public Integer call() throws Exception {
        try {
            new CodestartGenerator(output).generate(getCodestart(), getOutputStrategySpec());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
