package io.quarkus.cli;

import java.util.Map;

import io.quarkus.cli.common.BuildToolContext;
import io.quarkus.cli.common.BuildToolDelegatingCommand;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Option;

@Command(name = "run", sortOptions = false, mixinStandardHelpOptions = false, header = "Run application.")
public class Run extends BuildToolDelegatingCommand {

    private static final Map<BuildTool, String> ACTION_MAPPING = Map.of(BuildTool.MAVEN, "quarkus:run",
            BuildTool.GRADLE, "quarkusRun", BuildTool.GRADLE_KOTLIN_DSL, "quarkusRun");

    @Option(names = { "--target" }, description = "Run target.")
    String target;

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        if (target != null)
            context.getPropertiesOptions().properties.put("quarkus.run.target", target);
    }

    @Override
    public Map<BuildTool, String> getActionMapping() {
        return ACTION_MAPPING;
    }

    @Override
    public String toString() {
        return "Run {}";
    }
}
