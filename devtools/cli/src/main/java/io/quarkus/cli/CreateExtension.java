package io.quarkus.cli;

import java.util.Map;
import java.util.TreeMap;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.cli.create.BaseCreateCommand;
import io.quarkus.cli.create.ExtensionGAVMixin;
import io.quarkus.cli.create.ExtensionNameGenerationGroup;
import io.quarkus.cli.create.ExtensionTestGenerationGroup;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateExtensionCommandHandler;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;
import picocli.CommandLine;

@CommandLine.Command(name = "extension", sortOptions = false, mixinStandardHelpOptions = false, showDefaultValues = true, header = "Create a Quarkus extension project", description = "%n"
        + "Quarkus extensions are built from multiple modules: runtime, deployment, integration-test and "
        + "docs. This command will generate a Maven multi-module project in a directory called EXTENSION-ID "
        + "by applying naming conventions to the specified EXTENSION-ID.", footer = { "%nDefault Naming conventions%n",
                " GROUP-ID: io.quarkiverse.<EXTENSION-ID>",
                " EXTENSION-NAME: EXTENSION-ID converted to Capitalized Words",
                " NAMESPACE-NAME: NAMESPACE-ID converted to Capitalized Words",
                "%nModule Naming Conventions%n",
                " parent: ",
                "    artifactId:\t[NAMESPACE-ID][EXTENSION-ID]-parent",
                "    name:\t[NAMESPACE-NAME][EXTENSION-NAME] - Parent",
                " runtime:",
                "    artifactId:\t[NAMESPACE-ID][EXTENSION-ID]",
                "    name:\t[NAMESPACE-NAME][EXTENSION-NAME] - Runtime",
                " deployment:",
                "    artifactId:\t[NAMESPACE-ID][EXTENSION-ID]-deployment",
                "    name:\t[NAMESPACE-NAME][EXTENSION-NAME] - Deployment",
                " integration-tests:",
                "    artifactId:\t[NAMESPACE-ID][EXTENSION-ID]-integration-tests",
                "    name:\t[NAMESPACE-NAME][EXTENSION-NAME] - Integration Tests",
                " docs:",
                "    artifactId:\t[NAMESPACE-ID][EXTENSION-ID]-docs",
                "    name:\t[NAMESPACE-NAME][EXTENSION-NAME] - Documentation",
                "%nPackage and Class Naming Conventions%n",
                " Package name: [GROUP-ID][EXTENSION-ID] with any dashes replaced by dots",
                " Class name prefix: EXTENSION-ID converted to CamelCase",
                "%nAs an example, specifying 'hello-world' as the EXTENSION-ID and "
                        + "'org.acme' as the GROUP-ID will generate a project containing the following modules:%n",
                "  hello-world: ",
                "    artifact:\torg.acme:hello-world-parent:1.0.0-SNAPSHOT",
                "    name:\tHello World - Parent",
                "  hello-world/runtime:",
                "    artifact:\torg.acme:hello-world:1.0.0-SNAPSHOT",
                "    name:\tHello World - Runtime",
                "    package name: org.acme.hello.world.runtime",
                "  hello-world/deployment:",
                "    artifact:\torg.acme:hello-world-deployment:1.0.0-SNAPSHOT",
                "    name:\tHello World - Deployment",
                "    package names: org.acme.hello.world.deployment, org.acme.hello.world.test",
                "  hello-world/integration-test:",
                "    artifact:\torg.acme:hello-world-integration-tests:1.0.0-SNAPSHOT",
                "    name:\tHello World - Integration Tests",
                "    package name: org.acme.hello.world.it",
                "  hello-world/docs:",
                "    artifact:\torg.acme:hello-world-docs:1.0.0-SNAPSHOT",
                "    name:\tHello World - Documentation",
                "%nGenerated classes will use 'HelloWorld' as a class name prefix."
        }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class CreateExtension extends BaseCreateCommand {

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @CommandLine.Mixin
    ExtensionGAVMixin gav = new ExtensionGAVMixin();

    @CommandLine.ArgGroup(order = 1, heading = "%nQuarkus version:%n")
    TargetQuarkusVersionGroup targetQuarkusVersion = new TargetQuarkusVersionGroup();

    @CommandLine.ArgGroup(order = 2, exclusive = false, heading = "%nGenerated artifacts%n")
    ExtensionNameGenerationGroup nameGeneration = new ExtensionNameGenerationGroup();

    @CommandLine.ArgGroup(order = 3, exclusive = false, heading = "%nCode Generation (Optional):%n")
    ExtensionTestGenerationGroup testGeneration = new ExtensionTestGenerationGroup();

    @CommandLine.ArgGroup(order = 4, exclusive = false, validate = false)
    PropertiesOptions propertiesOptions = new PropertiesOptions();

    @Override
    public Integer call() throws Exception {
        try {
            output.debug("Creating a new extension project with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            setExtensionId(gav.getExtensionId());
            setTestOutputDirectory(output.getTestDirectory());
            if (checkProjectRootAlreadyExists(runMode.isDryRun())) {
                return CommandLine.ExitCode.USAGE;
            }

            BuildTool buildTool = BuildTool.MAVEN;
            QuarkusProject quarkusProject = getExtensionVersions(buildTool, targetQuarkusVersion);
            ExtensionCatalog catalog = quarkusProject.getExtensionsCatalog();
            ArtifactCoords quarkusBom = catalog.getBom();

            final CreateExtensionCommandHandler createExtension = new io.quarkus.devtools.commands.CreateExtension(
                    outputDirectory())
                            .extensionId(gav.getExtensionId())
                            .groupId(gav.getGroupId())
                            .version(gav.getVersion())
                            .extensionName(nameGeneration.getExtensionName())
                            .namespaceId(nameGeneration.getNamespaceId())
                            .namespaceName(nameGeneration.getNamespaceName())
                            .packageName(nameGeneration.getPackageName())
                            .quarkusVersion(catalog.getQuarkusCoreVersion())
                            .quarkusBomGroupId(quarkusBom.getGroupId())
                            .quarkusBomArtifactId(quarkusBom.getArtifactId())
                            .quarkusBomVersion(quarkusBom.getVersion())
                            .withoutUnitTest(testGeneration.skipUnitTest())
                            .withoutDevModeTest(testGeneration.skipDevModeTest())
                            .withoutIntegrationTests(testGeneration.skipIntegrationTests())
                            .prepare();

            QuarkusCommandOutcome outcome = QuarkusCommandOutcome.success();

            if (runMode.isDryRun()) {
                dryRun(buildTool, createExtension, output);
            } else { // maven or gradle
                outcome = createExtension.execute(output);
            }

            if (outcome.isSuccess()) {
                if (!runMode.isDryRun()) {
                    output.info(
                            "Navigate into this directory and get started: " + spec.root().qualifiedName() + " build");
                }
                return CommandLine.ExitCode.OK;
            }
            return CommandLine.ExitCode.SOFTWARE;
        } catch (Exception e) {
            output.error("Extension creation failed, " + e.getMessage());
            return output.handleCommandException(e,
                    "Unable to create extension: " + e.getMessage());
        }
    }

    public void dryRun(BuildTool buildTool, CreateExtensionCommandHandler invocation, OutputOptionMixin output) {
        CommandLine.Help help = spec.commandLine().getHelp();
        output.printText(new String[] {
                "\nA new extension would have been created in",
                "\t" + outputDirectory().toString(),
                "\nThe extension would have been created using the following settings:\n"
        });
        Map<String, String> dryRunOutput = new TreeMap<>();
        for (Map.Entry<String, Object> entry : invocation.getData().entrySet()) {
            dryRunOutput.put(prettyName(entry.getKey()), entry.getValue().toString());
        }
        dryRunOutput.put("Skip Unit Test", "" + testGeneration.skipUnitTest());
        dryRunOutput.put("Skip Dev-mode Test", "" + testGeneration.skipDevModeTest());
        dryRunOutput.put("Skip Integration Test", "" + testGeneration.skipIntegrationTests());
        output.info(help.createTextTable(dryRunOutput).toString());
    }

    @Override
    public String toString() {
        return "CreateExtension{" + "gav=" + gav
                + ", quarkusVersion=" + targetQuarkusVersion
                + ", nameGeneration=" + nameGeneration
                + ", testGeneration=" + testGeneration
                + '}';
    }
}
