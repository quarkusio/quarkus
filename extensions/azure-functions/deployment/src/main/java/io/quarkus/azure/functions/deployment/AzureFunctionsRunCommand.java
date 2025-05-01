package io.quarkus.azure.functions.deployment;

import static io.quarkus.azure.functions.deployment.AzureFunctionsDeployCommand.AZURE_FUNCTIONS;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalInt;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.CommandUtils;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.cmd.RunCommandActionBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class AzureFunctionsRunCommand {
    private static final Logger log = Logger.getLogger(AzureFunctionsRunCommand.class);
    protected static final String FUNC_CMD = "func -v";
    protected static final String FUNC_HOST_START_CMD = "func host start -p %s";
    protected static final String RUNTIME_NOT_FOUND = "Azure Functions Core Tools not found. " +
            "Please go to https://aka.ms/azfunc-install to install Azure Functions Core Tools first.";
    private static final String FUNC_HOST_START_WITH_DEBUG_CMD = "func host start -p %s --language-worker -- " +
            "\"-agentlib:jdwp=%s\"";
    private static final String STARTED_EXPRESSION = "Host lock lease acquired";

    @BuildStep
    public RunCommandActionBuildItem run(List<AzureFunctionBuildItem> functions, OutputTargetBuildItem target,
            AzureFunctionsAppNameBuildItem appName,
            AzureFunctionsConfig config) throws Exception {
        Path stagingDir = getDeploymentStagingDirectoryPath(target, appName.getAppName());
        File file = stagingDir.toFile();
        if (!file.exists() || !file.isDirectory()) {
            throw new BuildException("Staging directory does not exist.  Rebuild the app", Collections.emptyList());
        }

        final CommandHandler commandHandler = new CommandHandlerImpl();

        checkRuntimeExistence(commandHandler);

        String cmd = getStartFunctionHostCommand(config);
        List<String> args = new LinkedList<>();
        Arrays.stream(cmd.split(" ")).forEach(s -> args.add(s));
        RunCommandActionBuildItem launcher = new RunCommandActionBuildItem(AZURE_FUNCTIONS, args, stagingDir,
                STARTED_EXPRESSION, null,
                true);
        return launcher;
    }

    protected Path getDeploymentStagingDirectoryPath(OutputTargetBuildItem target, String appName) {
        return target.getOutputDirectory().resolve(AZURE_FUNCTIONS).resolve(appName);
    }

    protected void checkRuntimeExistence(final CommandHandler handler) throws AzureExecutionException {
        handler.runCommandWithReturnCodeCheck(
                getCheckRuntimeCommand(),
                true, /* showStdout */
                null, /* workingDirectory */
                CommandUtils.getDefaultValidReturnCodes(),
                RUNTIME_NOT_FOUND);
    }

    protected String getCheckRuntimeCommand() {
        return FUNC_CMD;
    }

    protected String getStartFunctionHostCommand(AzureFunctionsConfig azureConfig) {
        int funcPort;
        if (azureConfig.funcPort().isPresent()) {
            funcPort = azureConfig.funcPort().get();
        } else {
            Config config = ConfigProviderResolver.instance().getConfig();
            funcPort = config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(8081);
        }
        final String enableDebug = System.getProperty("enableDebug");
        if (StringUtils.isNotEmpty(enableDebug) && enableDebug.equalsIgnoreCase("true")) {
            return String.format(FUNC_HOST_START_WITH_DEBUG_CMD, funcPort, azureConfig.localDebugConfig());
        } else {
            return String.format(FUNC_HOST_START_CMD, funcPort);
        }
    }
}
