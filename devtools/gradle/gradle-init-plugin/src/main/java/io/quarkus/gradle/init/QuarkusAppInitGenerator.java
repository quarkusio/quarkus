package io.quarkus.gradle.init;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.buildinit.specs.BuildInitConfig;
import org.gradle.buildinit.specs.BuildInitGenerator;
import org.gradle.buildinit.specs.BuildInitParameter;

import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.registry.RegistryResolutionException;

public class QuarkusAppInitGenerator implements BuildInitGenerator {

    private static final String DEFAULT_PROJECT_NAME = "code-with-quarkus";
    private static final String DEFAULT_GROUP_ID = "org.acme";
    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String GROOVY_DSL = "groovy";

    @Inject
    public QuarkusAppInitGenerator() {
    }

    @Override
    public void generate(BuildInitConfig config, Directory targetDirectory) {
        String projectName = stringArgument(config, ProjectNameParameter.INSTANCE, DEFAULT_PROJECT_NAME);
        String groupId = stringArgument(config, GroupIdParameter.INSTANCE, DEFAULT_GROUP_ID);
        String version = stringArgument(config, StringInitParameter.VERSION, DEFAULT_VERSION);
        Set<String> extensions = parseExtensions(stringArgument(config, ExtensionsParameter.INSTANCE, ""));
        BuildTool buildTool = buildTool(stringArgument(config, DslParameter.INSTANCE, ""));
        String quarkusVersion = stringArgumentOrNull(config, StringInitParameter.QUARKUS_VERSION);
        Path targetPath = targetDirectory.getAsFile().toPath();

        try {
            QuarkusProject quarkusProject = resolveProject(targetPath, buildTool, quarkusVersion);
            new CreateProject(quarkusProject)
                    .groupId(groupId)
                    .artifactId(projectName)
                    .version(version)
                    .description(stringArgumentOrNull(config, StringInitParameter.DESCRIPTION))
                    .resourceClassName(stringArgumentOrNull(config, StringInitParameter.CLASS_NAME))
                    .resourcePath(stringArgumentOrNull(config, StringInitParameter.PATH))
                    .extensions(extensions)
                    .noDockerfiles(booleanArgument(config, BooleanInitParameter.NO_DOCKERFILES))
                    .noBuildToolWrapper(booleanArgument(config, BooleanInitParameter.NO_BUILD_TOOL_WRAPPER))
                    .noCode(booleanArgument(config, BooleanInitParameter.NO_CODE))
                    .execute();
        } catch (QuarkusCommandException | RegistryResolutionException e) {
            throw new GradleException("Failed to generate the Quarkus project", e);
        }
    }

    /**
     * Resolves the extension catalog explicitly instead of using the deprecated
     * {@link QuarkusProjectHelper#getProject(Path, BuildTool, String)} overload.
     */
    private QuarkusProject resolveProject(Path targetPath, BuildTool buildTool, String quarkusVersion)
            throws RegistryResolutionException {
        if (quarkusVersion == null) {
            return QuarkusProjectHelper.getProject(targetPath, buildTool);
        }
        return QuarkusProjectHelper.getProject(targetPath,
                QuarkusProjectHelper.getCatalogResolver().resolveExtensionCatalog(quarkusVersion), buildTool);
    }

    private String stringArgument(BuildInitConfig config, BuildInitParameter<String> parameter, String defaultValue) {
        Object value = config.getArguments().get(parameter);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }

    private String stringArgumentOrNull(BuildInitConfig config, BuildInitParameter<String> parameter) {
        Object value = config.getArguments().get(parameter);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return null;
    }

    private boolean booleanArgument(BuildInitConfig config, BuildInitParameter<Boolean> parameter) {
        Object value = config.getArguments().get(parameter);
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private BuildTool buildTool(String dsl) {
        return GROOVY_DSL.equalsIgnoreCase(dsl) ? BuildTool.GRADLE : BuildTool.GRADLE_KOTLIN_DSL;
    }

    private Set<String> parseExtensions(String extensions) {
        Set<String> result = new HashSet<>();
        StringTokenizer tokenizer = new StringTokenizer(extensions, ",");
        while (tokenizer.hasMoreTokens()) {
            String extension = tokenizer.nextToken().trim();
            if (!extension.isEmpty()) {
                result.add(extension);
            }
        }
        return result;
    }
}
