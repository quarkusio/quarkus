package io.quarkus.devtools.commands;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.APP_CONFIG;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.*;
import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateProjectCommandHandler;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;
import org.apache.commons.lang3.StringUtils;

/**
 * Instances of this class are not thread-safe. They are created per invocation.
 *
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class CreateProject {

    public static final String NAME = "create-project";

    public static final String NO_DOCKERFILES = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "no-dockerfiles");
    public static final String NO_BUILDTOOL_WRAPPER = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "no-buildtool-wrapper");
    public static final String NO_CODE = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "no-code");
    public static final String EXAMPLE = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "example");
    public static final String EXTRA_CODESTARTS = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "extra-codestarts");

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("(?:1\\.)?(\\d+)(?:\\..*)?");

    private QuarkusProject quarkusProject;
    private String javaTarget;
    private Set<String> extensions = new HashSet<>();

    private Map<String, Object> values = new HashMap<>();

    public CreateProject(QuarkusProject project) {
        this.quarkusProject = requireNonNull(project, "project is required");
    }

    public CreateProject groupId(String groupId) {
        setValue(PROJECT_GROUP_ID, groupId);
        return this;
    }

    public CreateProject artifactId(String artifactId) {
        setValue(PROJECT_ARTIFACT_ID, artifactId);
        return this;
    }

    public CreateProject version(String version) {
        setValue(PROJECT_VERSION, version);
        return this;
    }

    @Deprecated
    public CreateProject quarkusMavenPluginVersion(String version) {
        setValue(QUARKUS_MAVEN_PLUGIN_VERSION, version);
        return this;
    }

    @Deprecated
    public CreateProject quarkusGradlePluginVersion(String version) {
        setValue(QUARKUS_GRADLE_PLUGIN_VERSION, version);
        return this;
    }

    public CreateProject quarkusPluginVersion(String version) {
        if (quarkusProject.getBuildTool().equals(BuildTool.MAVEN)) {
            setValue(QUARKUS_MAVEN_PLUGIN_VERSION, version);
        } else {
            setValue(QUARKUS_GRADLE_PLUGIN_VERSION, version);
        }
        return this;
    }

    public CreateProject sourceType(SourceType sourceType) {
        setValue(SOURCE_TYPE, sourceType);
        return this;
    }

    public CreateProject extraCodestarts(Set<String> extraCodestarts) {
        setValue(EXTRA_CODESTARTS, extraCodestarts);
        return this;
    }

    public CreateProject javaTarget(String javaTarget) {
        this.javaTarget = javaTarget;
        return this;
    }

    public CreateProject resourcePath(String resourcePath) {
        setValue(RESOURCE_PATH, resourcePath);
        return this;
    }

    public CreateProject appConfig(String appConfigAsString) {
        Map<String, String> configMap = Collections.emptyMap();

        if (StringUtils.isNoneBlank(appConfigAsString)) {
            configMap = ToolsUtils.stringToMap(appConfigAsString, ",", "=");
        }
        setValue(APP_CONFIG.key(), configMap);
        return this;
    }

    /**
     * Use packageName instead as this one is only working with RESTEasy and SpringWeb
     */
    @Deprecated
    public CreateProject className(String className) {
        if (className == null) {
            return this;
        }
        if (!(SourceVersion.isName(className) && !SourceVersion.isKeyword(className))) {
            throw new IllegalArgumentException(className + " is not a valid class name");
        }
        setValue(CLASS_NAME, className);
        return this;
    }

    public CreateProject packageName(String packageName) {
        if (packageName == null) {
            return this;
        }
        if (!(SourceVersion.isName(packageName) && !SourceVersion.isKeyword(packageName))) {
            throw new IllegalArgumentException(packageName + " is not a  package name");
        }
        setValue(PACKAGE_NAME, packageName);
        return this;
    }

    public CreateProject extensions(Set<String> extensions) {
        if (extensions == null) {
            return this;
        }
        this.extensions.addAll(extensions);
        return this;
    }

    public CreateProject example(String example) {
        setValue(EXAMPLE, example);
        return this;
    }

    public CreateProject noCode(boolean value) {
        setValue(NO_CODE, value);
        return this;
    }

    public CreateProject noCode() {
        return noCode(true);
    }

    public CreateProject noBuildToolWrapper(boolean value) {
        setValue(NO_BUILDTOOL_WRAPPER, value);
        return this;
    }

    public CreateProject noBuildToolWrapper() {
        return noBuildToolWrapper(true);
    }

    public CreateProject noDockerfiles(boolean value) {
        setValue(NO_DOCKERFILES, value);
        return this;
    }

    public CreateProject noDockerfiles() {
        return noDockerfiles(true);
    }

    public CreateProject setValue(String name, Object value) {
        if (value != null) {
            values.put(name, value);
        }
        return this;
    }

    public boolean doCreateProject(final Map<String, Object> context) throws QuarkusCommandException {
        if (context != null && !context.isEmpty()) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (entry.getValue() != null) {
                    setValue(entry.getKey(), entry.getValue());
                }
            }
        }
        return execute().isSuccess();
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        // Define the Java version to use determined from the one specified or the one creating the project
        Matcher matcher = JAVA_VERSION_PATTERN
                .matcher(this.javaTarget != null ? this.javaTarget : System.getProperty("java.version", ""));
        if (matcher.matches() && Integer.parseInt(matcher.group(1)) < 11) {
            setValue(JAVA_TARGET, "8");
        } else {
            setValue(JAVA_TARGET, "11");
        }
        if (containsSpringWeb(extensions)) {
            setValue(IS_SPRING, true);
            if (containsRESTEasy(extensions)) {
                values.remove(CLASS_NAME);
                values.remove(RESOURCE_PATH);
            }
        }
        setValue(EXTENSIONS, extensions);
        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, values);
        return new CreateProjectCommandHandler().execute(invocation);
    }

    public static SourceType determineSourceType(Set<String> extensions) {
        Optional<SourceType> sourceType = extensions.stream()
                .map(SourceType::parse)
                .filter(Optional::isPresent)
                .map(e -> e.orElse(SourceType.JAVA))
                .findAny();
        return sourceType.orElse(SourceType.JAVA);
    }

    private static boolean containsSpringWeb(Collection<String> extensions) {
        return extensions.stream().anyMatch(e -> e.toLowerCase().contains("spring-web"));
    }

    private static boolean containsRESTEasy(Collection<String> extensions) {
        return extensions.isEmpty() || extensions.stream().anyMatch(e -> e.toLowerCase().contains("resteasy"));
    }
}
