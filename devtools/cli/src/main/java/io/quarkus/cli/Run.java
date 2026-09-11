package io.quarkus.cli;

import java.util.Map;

import io.quarkus.cli.common.BuildToolContext;
import io.quarkus.cli.common.BuildToolDelegatingCommand;
import io.quarkus.cli.common.DebugOptions;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "run", sortOptions = false, mixinStandardHelpOptions = false, header = "Run application.")
public class Run extends BuildToolDelegatingCommand {

    private static final Map<BuildTool, String> ACTION_MAPPING = Map.of(BuildTool.MAVEN, "quarkus:run",
            BuildTool.GRADLE, "quarkusRun", BuildTool.GRADLE_KOTLIN_DSL, "quarkusRun");

    @CommandLine.Option(names = { "--target" }, description = "Run target.")
    String target;

    @CommandLine.ArgGroup(order = 3, exclusive = false, validate = true, heading = "%nDebug options:%n")
    DebugOptions debugOptions = new DebugOptions();

    // Debug is off by default for run mode (unlike dev mode)
    {
        debugOptions.debug = false;
    }

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        if (target != null)
            context.getPropertiesOptions().properties.put("quarkus.run.target", target);
        if (debugOptions.debug) {
            String jdwpArg = debugOptions.getJvmDebugParameter();
            BuildTool buildTool = context.getBuildTool();
            if (buildTool == BuildTool.GRADLE || buildTool == BuildTool.GRADLE_KOTLIN_DSL) {
                context.getParams().add("--jvm-args=" + jdwpArg);
            } else {
                context.getPropertiesOptions().properties.put("jvmArgs", jdwpArg);
            }
        }
    }

    @Override
    public Map<BuildTool, String> getActionMapping() {
        return ACTION_MAPPING;
    }

    @Override
    public String toString() {
        return "Run [debugOptions=" + debugOptions + "]";
    }
}
