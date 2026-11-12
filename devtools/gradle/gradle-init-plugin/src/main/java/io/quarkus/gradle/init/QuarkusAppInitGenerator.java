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

public class QuarkusAppInitGenerator implements BuildInitGenerator {

    private static final String DEFAULT_PROJECT_NAME = "code-with-quarkus";
    private static final String DEFAULT_GROUP_ID = "org.acme";
    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String GROOVY_DSL = "groovy";
    private static final String KOTLIN_DSL = "kotlin";

    @Inject
    public QuarkusAppInitGenerator() {
    }

    /**
     * Always skips this project's own build tool wrapper: {@code gradle init} generates its own wrapper into
     * {@code targetDirectory} right after this method returns, regardless of what a spec generator produced, so
     * generating one here would only be overwritten.
     * <p>
     * The project is resolved through the {@code quarkusVersion} overload of {@link QuarkusProjectHelper} because it
     * is the one honoring the {@code quarkusRegistryClient} property.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void generate(BuildInitConfig config, Directory targetDirectory) {
        String projectName = stringArgument(config, StringInitParameter.PROJECT_NAME, DEFAULT_PROJECT_NAME);
        String groupId = stringArgument(config, StringInitParameter.GROUP_ID, DEFAULT_GROUP_ID);
        String version = stringArgument(config, StringInitParameter.VERSION, DEFAULT_VERSION);
        Set<String> extensions = parseExtensions(stringArgument(config, StringInitParameter.EXTENSIONS, ""));
        BuildTool buildTool = buildTool(stringArgument(config, StringInitParameter.DSL, KOTLIN_DSL));
        String quarkusVersion = stringArgumentOrNull(config, StringInitParameter.QUARKUS_VERSION);
        Path targetPath = targetDirectory.getAsFile().toPath();

        try {
            QuarkusProject quarkusProject = QuarkusProjectHelper.getProject(targetPath, buildTool, quarkusVersion);
            new CreateProject(quarkusProject)
                    .groupId(groupId)
                    .artifactId(projectName)
                    .version(version)
                    .description(stringArgumentOrNull(config, StringInitParameter.DESCRIPTION))
                    .resourceClassName(stringArgumentOrNull(config, StringInitParameter.CLASS_NAME))
                    .resourcePath(stringArgumentOrNull(config, StringInitParameter.PATH))
                    .extensions(extensions)
                    .noDockerfiles(booleanArgument(config, BooleanInitParameter.NO_DOCKERFILES))
                    .noBuildToolWrapper(true)
                    .noCode(booleanArgument(config, BooleanInitParameter.NO_CODE))
                    .execute();
        } catch (QuarkusCommandException | RuntimeException e) {
            throw new GradleException("Failed to generate the Quarkus project", e);
        }
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
        if (GROOVY_DSL.equalsIgnoreCase(dsl)) {
            return BuildTool.GRADLE;
        }
        if (KOTLIN_DSL.equalsIgnoreCase(dsl)) {
            return BuildTool.GRADLE_KOTLIN_DSL;
        }
        throw new GradleException("Unsupported dsl '" + dsl + "', expected '" + GROOVY_DSL + "' or '" + KOTLIN_DSL + "'");
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
