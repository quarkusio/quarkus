package io.quarkus.cli;

import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.cli.create.BaseCreateCommand;
import io.quarkus.cli.create.CreateProjectMixin;
import io.quarkus.cli.create.ExtensionNameGenerationGroup;
import io.quarkus.cli.create.ExtensionTargetGVGroup;
import io.quarkus.cli.create.ExtensionTestGenerationGroup;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.codegen.SourceType;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;

@CommandLine.Command(name = "extension", sortOptions = false, mixinStandardHelpOptions = false, header = "Create a Quarkus extension project", description = "%n"
        + "Quarkus extensions are built from multiple modules: runtime, deployment, and "
        + "integration-test. This command will generate a Maven multi-module project in a directory called EXTENSION-ID "
        + " by applying naming conventions to the specified EXTENSION-ID.%n", footer = { "%nDefault Naming conventions%n",
                " EXTENSION-NAME: EXTENSION-ID converted to Capitalized Words",
                " NAMESPACE-NAME: NAMESPACE-ID converted to Capitalized Words", "%nModule Naming Conventions%n",
                " parent: ", "    artifactId:\t[NAMESPACE-ID][EXTENSION-ID]-parent",
                "    name:\t[NAMESPACE-NAME][EXTENSION-NAME] - Parent", " runtime:",
                "    artifactId:\t[NAMESPACE-ID][EXTENSION-ID]",
                "    name:\t[NAMESPACE-NAME][EXTENSION-NAME] - Runtime", " deployment:",
                "    artifactId:\t[NAMESPACE-ID][EXTENSION-ID]-deployment",
                "    name:\t[NAMESPACE-NAME][EXTENSION-NAME] - Deployment", " integration-tests:",
                "    artifactId:\t[NAMESPACE-ID][EXTENSION-ID]-integration-tests",
                "    name:\t[NAMESPACE-NAME][EXTENSION-NAME] - Integration Tests",
                "%nPackage and Class Naming Conventions%n",
                " Package name: [GROUP-ID][EXTENSION-ID] with any dashes replaced by dots",
                " Class name prefix: EXTENSION-ID converted to CamelCase",
                "%nAs an example, specifying 'hello-world' as the EXTENSION-ID and "
                        + "'org.acme' as the GROUP-ID will generate a project containing the following modules:%n",
                "  hello-world: ", "    artifact:\torg.acme:hello-world-parent:1.0.0-SNAPSHOT",
                "    name:\tHello World - Parent", "  hello-world/runtime:",
                "    artifact:\torg.acme:hello-world:1.0.0-SNAPSHOT", "    name:\tHello World - Runtime",
                "    package name: org.acme.hello.world.runtime", "  hello-world/deployment:",
                "    artifact:\torg.acme:hello-world-deployment:1.0.0-SNAPSHOT", "    name:\tHello World - Deployment",
                "    package names: org.acme.hello.world.deployment, org.acme.hello.world.test",
                "  hello-world/integration-test:",
                "    artifact:\torg.acme:hello-world-integration-tests:1.0.0-SNAPSHOT",
                "    name:\tHello World - Integration Tests", "    package name: org.acme.hello.world.it",
                "%nGenerated classes will use 'HelloWorld' as a class name prefix." }, showDefaultValues = true)
public class CreateExtension extends BaseCreateCommand {

    @Mixin
    CreateProjectMixin createProject;

    @CommandLine.ArgGroup(order = 1, exclusive = false, heading = "%nExtension group and version%n", validate = false)
    ExtensionTargetGVGroup gv = new ExtensionTargetGVGroup();

    @CommandLine.ArgGroup(order = 2, heading = "%nQuarkus version%n")
    TargetQuarkusVersionGroup quarkusVersion = new TargetQuarkusVersionGroup();

    @CommandLine.ArgGroup(order = 3, exclusive = false, heading = "%nGenerated artifacts%n")
    ExtensionNameGenerationGroup nameGeneration = new ExtensionNameGenerationGroup();

    @CommandLine.ArgGroup(order = 5, exclusive = false, heading = "%nGenerated code (Optional)%n")
    ExtensionTestGenerationGroup testGeneration = new ExtensionTestGenerationGroup();

    @CommandLine.Parameters(paramLabel = "EXTENSION-ID", description = "Identifier used to generate module identifiers.")
    String extensionId;

    @Override
    public Integer call() throws Exception {
        try {
            output.debug("Creating a new project with initial parameters: %s", this);

            //createProject.setExtensionProjectGV(gv);
            createProject.projectRoot(); // verify project directories early

            BuildTool buildTool = BuildTool.MAVEN;
            SourceType sourceType = SourceType.JAVA;

            //QuarkusCommandInvocation invocation = createProject.build(buildTool, targetQuarkusVersion, output);
            boolean success = true;

            if (runMode.isDryRun()) {
                //createProject.dryRun(buildTool, invocation, output);
            } else {
                //success = new CreateExtensionCommandHandler().execute(log, groupId, artifactId, input, newExtensionDir)
            }
            return success ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
        } catch (Exception e) {
            output.error("Project creation failed, " + e.getMessage());
            return output.handleCommandException(e,
                    "Unable to create project: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "CreateExtension{" + "gv=" + gv
                + ", quarkusVersion=" + quarkusVersion
                + ", nameGeneration=" + nameGeneration
                + ", testGeneration=" + testGeneration
                + ", extensionId='" + extensionId + '\'' + '}';
    }
}
