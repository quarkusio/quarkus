package io.quarkus.cli.create;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.RegistryClientMixin;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import io.quarkus.devtools.project.codegen.ProjectGenerator;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.registry.RegistryResolutionException;
import picocli.CommandLine.Help;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

public class CreateProjectMixin {
    Map<String, Object> values = new HashMap<>();
    Path outputPath;
    Path projectRootPath;
    String projectDirName;

    @Spec(Spec.Target.MIXEE)
    CommandSpec mixee;

    @Option(paramLabel = "OUTPUT-DIR", names = { "-o",
            "--output-directory" }, description = "The directory to create the new project in.")
    String targetDirectory;

    @Mixin
    RegistryClientMixin registryClient;

    public void setTestOutputDirectory(Path testOutputDirectory) {
        if (testOutputDirectory != null && targetDirectory == null) {
            outputPath = testOutputDirectory;
        }
    }

    public Path outputDirectory() {
        if (outputPath == null) {
            outputPath = CreateProjectHelper.createOutputDirectory(targetDirectory);
        }
        return outputPath;
    }

    public Path projectRoot() {
        if (projectRootPath == null) {
            requireNonNull(projectDirName,
                    "Must configure project directory name (e.g. using setSingleProjectGAV or equivalent");
            projectRootPath = CreateProjectHelper.checkProjectRootPath(outputDirectory(), projectDirName);
        }
        return projectRootPath;
    }

    public void setSourceTypeExtensions(Set<String> extensions, SourceType sourceType) {
        extensions = CreateProjectHelper.sanitizeExtensions(extensions);
        CreateProjectHelper.addSourceTypeExtensions(extensions, sourceType);

        setValue(ProjectGenerator.SOURCE_TYPE, sourceType);
        setValue(ProjectGenerator.EXTENSIONS, extensions);
    }

    public void setSingleProjectGAV(TargetGAVGroup targetGav) {
        projectDirName = targetGav.getArtifactId();

        setValue(ProjectGenerator.PROJECT_GROUP_ID, targetGav.getGroupId());
        setValue(ProjectGenerator.PROJECT_ARTIFACT_ID, targetGav.getArtifactId());
        setValue(ProjectGenerator.PROJECT_VERSION, targetGav.getVersion());
    }

    public void setCodegenOptions(CodeGenerationGroup codeGeneration) {
        setValue(ProjectGenerator.PACKAGE_NAME, codeGeneration.packageName);
        setValue(ProjectGenerator.APP_CONFIG, codeGeneration.getAppConfig());

        // TODO: Can we drop the negative constant? Can we use the same one for JBang?
        setValue(CreateProject.NO_BUILDTOOL_WRAPPER, !codeGeneration.includeWrapper);
    }

    public void setValue(String name, Object value) {
        if (value != null) {
            values.put(name, value);
        }
    }

    public QuarkusCommandInvocation build(BuildTool buildTool, TargetQuarkusVersionGroup targetVersion,
            OutputOptionMixin log, Map<String, String> properties)
            throws RegistryResolutionException {

        // TODO: Allow this to be configured? infer from active Java version?
        CreateProjectHelper.setJavaVersion(values, null);
        CreateProjectHelper.handleSpringConfiguration(values);
        log.debug("Creating an app using the following settings: %s", values);

        if (buildTool == null) {
            // Adapt some settings for JBang
            setValue("noJBangWrapper", values.get(CreateProject.NO_BUILDTOOL_WRAPPER));
            // TODO: JBang should be a proper build tool
            buildTool = BuildTool.MAVEN;
        }

        QuarkusProject qp = registryClient.createQuarkusProject(projectRoot(), targetVersion, buildTool, log);

        properties.entrySet().forEach(x -> {
            if (x.getValue().length() > 0) {
                System.setProperty(x.getKey(), x.getValue());
                log.info("property: %s=%s", x.getKey(), x.getValue());
            } else {
                System.setProperty(x.getKey(), "");
                log.info("property: %s", x.getKey());
            }
        });
        return new QuarkusCommandInvocation(qp, values);
    }

    @Override
    public String toString() {
        return "CreateProjectMixin ["
                + "outputPath=" + outputPath
                + ", registryClient" + registryClient
                + ", projectDirName=" + projectDirName
                + ", projectRootPath=" + projectRootPath
                + ", targetDirectory=" + targetDirectory
                + ", values=" + values + "]";
    }

    public void dryRun(BuildTool buildTool, QuarkusCommandInvocation invocation, OutputOptionMixin output) {
        Help help = mixee.commandLine().getHelp();
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

    String prettyName(String key) {
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
