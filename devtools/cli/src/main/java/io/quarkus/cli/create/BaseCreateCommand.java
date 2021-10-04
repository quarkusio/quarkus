package io.quarkus.cli.create;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.cli.registry.ToggleRegistryClientMixin;
import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import io.quarkus.devtools.project.codegen.ProjectGenerator;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.registry.RegistryResolutionException;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public class BaseCreateCommand implements Callable<Integer> {
    @Mixin
    protected RunModeOption runMode;

    @Mixin
    protected OutputOptionMixin output;

    @Mixin
    ToggleRegistryClientMixin registryClient;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @Spec
    protected CommandSpec spec;

    @CommandLine.Option(paramLabel = "OUTPUT-DIR", names = { "-o",
            "--output-directory" }, description = "The directory to create the new project in.")
    String targetDirectory;

    /**
     * Parameters gathered for project creation
     */
    private Map<String, Object> values = new HashMap<>();

    /**
     * Output path. Computed from {@link #targetDirectory} parameter.
     * 
     * @see #outputDirectory()
     */
    private Path outputPath;

    /**
     * Project root directory. Computed from/within Output path.
     * 
     * @see #checkProjectRootAlreadyExists(boolean)
     * @see #projectRoot()
     */
    private Path projectRootPath;

    /**
     * Project directory name, used with {@link #outputPath} to
     * compute {@link #projectRootPath}.
     * 
     * @see #setExtensionId(String)
     * @see #setSingleProjectGAV(TargetGAVGroup)
     */
    private String projectDirName;

    /**
     * If a targetDirectory parameter was not specified, and this was,
     * set this as output path.
     *
     * @param testOutputDirectory The path to use as the output directory if the target directory was not specified, or null
     */
    public void setTestOutputDirectory(Path testOutputDirectory) {
        if (testOutputDirectory != null && targetDirectory == null) {
            outputPath = testOutputDirectory;
        }
    }

    /**
     * Resolve (and create, if necessary) the target output directory.
     * 
     * @return the output directory path
     */
    public Path outputDirectory() {
        if (outputPath == null) {
            outputPath = CreateProjectHelper.createOutputDirectory(targetDirectory);
        }
        return outputPath;
    }

    /**
     * @param targetGav Group, Artifact, and Version for the single-module project.
     *        The artifactId is used as the directory name.
     */
    public void setSingleProjectGAV(TargetGAVGroup targetGav) {
        projectDirName = targetGav.getArtifactId();

        setValue(ProjectGenerator.PROJECT_GROUP_ID, targetGav.getGroupId());
        setValue(ProjectGenerator.PROJECT_ARTIFACT_ID, targetGav.getArtifactId());
        setValue(ProjectGenerator.PROJECT_VERSION, targetGav.getVersion());
    }

    /**
     * @param extensionId Extension id to be used as project directory name
     */
    public void setExtensionId(String extensionId) {
        projectDirName = extensionId;
    }

    /**
     * Resolve and remember the configured project directory.
     *
     * @param dryRun If true, warn that the directory already exists; otherwise, print an error message.
     * @return true IFF configured project root directory already exists and this is not a dry run;
     *         in other words, return true if caller should exit with an error.
     */
    public boolean checkProjectRootAlreadyExists(boolean dryRun) {
        if (projectRootPath == null) {
            try {
                projectRootPath = CreateProjectHelper.checkProjectRootPath(outputDirectory(), projectDirName);
            } catch (IllegalArgumentException iex) {
                if (dryRun) {
                    output.warn("A directory named '" + projectDirName + "' already exists.");
                    projectRootPath = outputDirectory().resolve(projectDirName);
                } else {
                    output.error(iex.getMessage());
                    output.out().printf("Specify a different artifactId / directory name.%n");
                    output.out().printf("See '%s --help' for more information.%n", spec.qualifiedName());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolve the target output directory. Will throw if the target directory exists
     * 
     * @return the project root path
     */
    public Path projectRoot() {
        if (projectRootPath == null) {
            projectRootPath = CreateProjectHelper.checkProjectRootPath(outputDirectory(), projectDirName);
        }
        return projectRootPath;
    }

    /**
     * Add explicitly specified and sourceType-implied extensions
     * 
     * @param extensions Explicitly specified extensions
     * @param sourceType Type of source (Kotlin, Java, Scala)
     */
    public void setSourceTypeExtensions(Set<String> extensions, SourceType sourceType) {
        extensions = CreateProjectHelper.sanitizeExtensions(extensions);
        CreateProjectHelper.addSourceTypeExtensions(extensions, sourceType);

        setValue(ProjectGenerator.SOURCE_TYPE, sourceType);
        setValue(ProjectGenerator.EXTENSIONS, extensions);
    }

    /**
     * Process code generation options (save values)
     * 
     * @param codeGeneration
     */
    public void setCodegenOptions(CodeGenerationGroup codeGeneration) {
        setValue(ProjectGenerator.PACKAGE_NAME, codeGeneration.packageName);
        setValue(ProjectGenerator.APP_CONFIG, codeGeneration.getAppConfig());

        setValue(CreateProject.NO_CODE, !codeGeneration.includeCode);
        setValue(CreateProject.NO_BUILDTOOL_WRAPPER, !codeGeneration.includeWrapper);
    }

    private void setValue(String name, Object value) {
        if (value != null) {
            values.put(name, value);
        }
    }

    /**
     * Create a new single-module project.
     * 
     * @param buildTool The build tool the project should use (maven, gradle, jbang)
     * @param targetVersion The target quarkus version
     * @param properties Additional properties that should be used whiel creating the properties
     * @return Quarkus command invocation that can be printed (dry-run) or run to create the project
     * @throws RegistryResolutionException
     */
    public QuarkusCommandInvocation build(BuildTool buildTool, TargetQuarkusVersionGroup targetVersion,
            Map<String, String> properties)
            throws RegistryResolutionException {

        // TODO: Allow the Java version to be configured? infer from active Java version?
        CreateProjectHelper.setJavaVersion(values, null);
        CreateProjectHelper.handleSpringConfiguration(values);
        output.debug("Creating an app using the following settings: %s", values);

        QuarkusProject qp = registryClient.createQuarkusProject(projectRoot(), targetVersion, buildTool, output);

        // TODO: knock on effect with properties.. here?
        properties.entrySet().forEach(x -> {
            if (x.getValue().length() > 0) {
                System.setProperty(x.getKey(), x.getValue());
                output.info("property: %s=%s", x.getKey(), x.getValue());
            } else {
                System.setProperty(x.getKey(), "");
                output.info("property: %s", x.getKey());
            }
        });
        return new QuarkusCommandInvocation(qp, values);
    }

    /**
     * @param buildTool The build tool the project should use (maven, gradle, jbang)
     * @param targetVersion The target quarkus version
     * @return Resolved QuarkusProject for the given build tool and target quarkus version
     * @throws RegistryResolutionException
     */
    public QuarkusProject getExtensionVersions(BuildTool buildTool, TargetQuarkusVersionGroup targetVersion)
            throws RegistryResolutionException {
        return registryClient.createQuarkusProject(outputDirectory(), targetVersion, buildTool, output);
    }

    @Override
    public Integer call() throws Exception {
        spec.commandLine().usage(output.out());
        return CommandLine.ExitCode.OK;
    }

    public String toString() {
        return "BaseCreateCommand ["
                + "outputPath=" + outputPath
                + ", registryClient" + registryClient
                + ", projectDirName=" + projectDirName
                + ", projectRootPath=" + projectRootPath
                + ", targetDirectory=" + targetDirectory
                + ", values=" + values + "]";
    }

    public void dryRun(BuildTool buildTool, QuarkusCommandInvocation invocation, OutputOptionMixin output) {
        CommandLine.Help help = spec.commandLine().getHelp();
        output.printText(new String[] {
                "\nA new project would have been created in",
                "\t" + projectRoot().toString(),
                "\nThe project would have been created using the following settings:\n"
        });
        Map<String, String> dryRunOutput = new TreeMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            dryRunOutput.put(prettyName(entry.getKey()), entry.getValue().toString());
        }
        dryRunOutput.put("Quarkus Core Version", invocation.getExtensionsCatalog().getQuarkusCoreVersion());
        dryRunOutput.put("Build tool", buildTool == null ? "JBang" : buildTool.name());
        output.info(help.createTextTable(dryRunOutput).toString());
    }

    public String prettyName(String key) {
        if (CreateProject.NO_BUILDTOOL_WRAPPER.equals(key)) {
            return "Omit build tool wrapper";
        }

        key = key.substring(0, 1).toUpperCase() + key.substring(1);
        StringBuilder builder = new StringBuilder(key);
        for (int i = 0; i < builder.length(); i++) {
            // Check char is underscore
            if (builder.charAt(i) == '_') {
                builder.replace(i, i + 1, " ");
                builder.replace(i + 1, i + 2,
                        String.valueOf(Character.toUpperCase(builder.charAt(i + 1))));
            }
        }
        return builder.toString();
    }
}
