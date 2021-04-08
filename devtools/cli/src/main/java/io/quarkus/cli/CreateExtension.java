package io.quarkus.cli;

import static io.quarkus.devtools.commands.CreateExtension.extractQuarkiverseExtensionId;
import static io.quarkus.devtools.commands.CreateExtension.isQuarkiverseGroupId;
import static io.quarkus.devtools.commands.CreateExtension.resolveModel;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.apache.maven.model.Model;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.utils.Prompter;
import picocli.CommandLine;

@CommandLine.Command(name = "create-extension", sortOptions = false, mixinStandardHelpOptions = false, description = "Creates the base of a Quarkus extension in different layout depending of the options and environment.")
public class CreateExtension extends BaseSubCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-g",
            "--group-id" }, order = 1, paramLabel = "GROUP-ID", description = "The groupId for project.")
    String groupId;

    @CommandLine.Option(names = { "-i",
            "--extension-id" }, order = 0, paramLabel = "EXTENSION-ID", description = "The extension id.")
    String extensionId;

    @CommandLine.Option(names = { "-v",
            "--version" }, order = 3, paramLabel = "VERSION", description = "The version for the extension.")
    String version = "1.0.0-SNAPSHOT";

    @CommandLine.Option(names = { "-q",
            "--quarkus-version" }, order = 4, paramLabel = "QUARKUS-VERSION", description = "The quarkus version for the extension.")
    String quarkusVersion;

    @CommandLine.Option(names = { "-N",
            "--namespace-id" }, order = 5, paramLabel = "NAMESPACE-ID", description = "A prefix common to all extension modules artifactIds.")
    String namespaceId;

    @CommandLine.Option(names = { "-p",
            "--package-name" }, order = 6, paramLabel = "PACKAGE-NAME", description = "Base package under which classes should be created in Runtime and Deployment modules.. When specified, use a custom package name instead of auto generating it from other parameters.")
    String packageName;

    @CommandLine.Option(names = {
            "--extension-name" }, order = 7, paramLabel = "EXTENSION-NAME", description = "When specified, use a custom extension name instead of generating it from the extension id.")
    String extensionName;

    @CommandLine.Option(names = {
            "--namespace-name" }, order = 8, paramLabel = "NAMESPACE-NAME", description = "A prefix common to all extension modules names. When specified, use a custom namespace name instead of generating it from the namespace id.")
    String namespaceName;

    @CommandLine.Option(names = {
            "--bom-group-id" }, order = 9, paramLabel = "BOM-GROUP-ID", description = "The group id of the Quarkus platform BOM.")
    String quarkusBomGroupId;

    @CommandLine.Option(names = {
            "--bom-artifact-id" }, order = 10, paramLabel = "BOM-ARTIFACT-ID", description = "The artifact id of the Quarkus platform BOM.")
    String quarkusBomArtifactId;

    @CommandLine.Option(names = {
            "--bom-version" }, order = 11, paramLabel = "BOM-VERSION", description = "The version id of the Quarkus platform BOM.")
    String quarkusBomVersion;

    @CommandLine.Option(names = {
            "--without-unit-test" }, order = 12, description = "Indicates whether to generate a unit test class for the extension.")
    boolean withoutUnitTest;

    @CommandLine.Option(names = {
            "--without-it-tests" }, order = 13, description = "Indicates whether to generate a integration tests for the extension.")
    boolean withoutIntegrationTests;

    @CommandLine.Option(names = {
            "--without-devmode-test" }, order = 14, description = "Indicates whether to generate a dev mode test class for the extension.")
    boolean withoutDevModeTest;

    @CommandLine.Option(names = {
            "--without-tests" }, order = 15, description = "Indicates whether to generate any test for the extension.")
    boolean withoutTests;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec; //

    @Override
    public Integer call() throws Exception {
        try {
            Path dir = Paths.get(System.getProperty("user.dir"));

            promptValues(dir);
            autoComputeQuarkiverseExtensionId();

            final io.quarkus.devtools.commands.CreateExtension createExtension = new io.quarkus.devtools.commands.CreateExtension(
                    dir)
                            .extensionId(extensionId)
                            .extensionName(extensionName)
                            .groupId(groupId)
                            .version(version)
                            .packageName(packageName)
                            .namespaceId(namespaceId)
                            .namespaceName(namespaceName)
                            .quarkusVersion(quarkusVersion)
                            .quarkusBomGroupId(quarkusBomGroupId)
                            .quarkusBomArtifactId(quarkusBomArtifactId)
                            .quarkusBomGroupId(quarkusBomVersion)
                            .withoutUnitTest(withoutTests || withoutUnitTest)
                            .withoutDevModeTest(withoutTests || withoutDevModeTest)
                            .withoutIntegrationTests(withoutTests || withoutIntegrationTests);

            try {
                boolean success = createExtension.execute().isSuccess();
                if (!success) {
                    err().println("Failed to generate Extension");
                    return CommandLine.ExitCode.SOFTWARE;
                }
            } catch (QuarkusCommandException e) {
                err().println("Failed to generate Extension, " + e.getMessage());
                if (parent.showErrors) {
                    e.printStackTrace(err());
                }
                return CommandLine.ExitCode.SOFTWARE;
            }

        } catch (Exception e) {
            err().println("Failed to generate Extension, " + e.getMessage());
            if (parent.showErrors) {
                e.printStackTrace(err());
            }
            return CommandLine.ExitCode.SOFTWARE;
        }

        return CommandLine.ExitCode.OK;
    }

    private void autoComputeQuarkiverseExtensionId() {
        if (isQuarkiverseGroupId(groupId) && isEmpty(extensionId)) {
            extensionId = extractQuarkiverseExtensionId(groupId);
        }
    }

    private void promptValues(Path dir) throws QuarkusCommandException {
        if (batchMode) {
            if (isBlank(extensionId)) {
                throw new CommandLine.MissingParameterException(spec.commandLine(), spec.optionsMap().get("-id"),
                        "Missing required option: '--extension-id=EXTENSION-ID'");
            }
            return;
        }
        try {
            final Prompter prompter = new Prompter();
            final Model model = resolveModel(dir);
            if (model == null || !model.getArtifactId().endsWith("quarkus-parent")) {
                if (isBlank(quarkusVersion)) {
                    quarkusVersion = prompter.prompt("Set the Quarkus version");
                }
                if (isBlank(groupId)) {
                    groupId = prompter.promptWithDefaultValue("Set the extension groupId", "org.acme");
                }
            }
            autoComputeQuarkiverseExtensionId();
            if (isBlank(extensionId)) {
                extensionId = prompter.prompt("Set the extension id");
            }
        } catch (IOException e) {
            throw new QuarkusCommandException("Unable to get user input", e);
        }
    }
}
