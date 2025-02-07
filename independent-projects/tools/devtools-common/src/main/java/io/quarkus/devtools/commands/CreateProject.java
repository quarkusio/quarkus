package io.quarkus.devtools.commands;

import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.APP_CONFIG;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.DATA;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.EXAMPLE;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.EXTENSIONS;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.EXTRA_CODESTARTS;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.NO_BUILDTOOL_WRAPPER;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.NO_CODE;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.NO_DOCKERFILES;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.PACKAGE_NAME;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.PROJECT_ARTIFACT_ID;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.PROJECT_DESCRIPTION;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.PROJECT_GROUP_ID;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.PROJECT_NAME;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.PROJECT_VERSION;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.RESOURCE_CLASS_NAME;
import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.RESOURCE_PATH;
import static io.quarkus.devtools.commands.handlers.CreateProjectCodestartDataConverter.PlatformPropertiesKey.QUARKUS_GRADLE_PLUGIN_VERSION;
import static io.quarkus.devtools.commands.handlers.CreateProjectCodestartDataConverter.PlatformPropertiesKey.QUARKUS_MAVEN_PLUGIN_VERSION;
import static io.quarkus.devtools.project.JavaVersion.computeJavaVersion;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateProjectCommandHandler;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.SourceType;
import io.quarkus.platform.tools.ToolsUtils;

/**
 * Instances of this class are not thread-safe. They are created per invocation.
 *
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class CreateProject {

    public interface CreateProjectKey {
        String PROJECT_GROUP_ID = "project.group-id";
        String PROJECT_ARTIFACT_ID = "project.artifact-id";
        String PROJECT_VERSION = "project.version";
        String PROJECT_NAME = "project.name";
        String PROJECT_DESCRIPTION = "project.description";
        String PACKAGE_NAME = "project.package-name";
        String EXTENSIONS = "project.extensions";
        String RESOURCE_CLASS_NAME = "project.resource.class-name";
        String RESOURCE_PATH = "project.resource.path";
        String JAVA_VERSION = "project.java-version";
        String APP_CONFIG = "project.app-config";

        String QUARKUS_VERSION = "quarkus-version";
        String NO_DOCKERFILES = "codegen.no-dockerfiles";
        String NO_BUILDTOOL_WRAPPER = "codegen.no-buildtool-wrapper";
        String NO_CODE = "codegen.no-code";
        String EXAMPLE = "codegen.example";
        String EXTRA_CODESTARTS = "codegen.extra-codestarts";

        String DATA = "data";
    }

    private QuarkusProject quarkusProject;
    private String javaVersion;
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

    public CreateProject name(String name) {
        if (name == null || name.isBlank()) {
            return this;
        }
        setValue(PROJECT_NAME, name);
        return this;
    }

    public CreateProject description(String description) {
        if (description == null || description.isBlank()) {
            return this;
        }
        setValue(PROJECT_DESCRIPTION, description);
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

    public CreateProject extraCodestarts(Set<String> extraCodestarts) {
        setValue(EXTRA_CODESTARTS, extraCodestarts);
        return this;
    }

    public CreateProject javaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
        return this;
    }

    public CreateProject resourcePath(String resourcePath) {
        setValue(RESOURCE_PATH, resourcePath);
        return this;
    }

    public CreateProject resourceClassName(String resourceClassName) {
        if (resourceClassName == null) {
            return this;
        }
        CreateProjectHelper.checkClassName(resourceClassName);
        setValue(RESOURCE_CLASS_NAME, resourceClassName);
        return this;
    }

    public CreateProject appConfig(String appConfigAsString) {
        Map<String, String> configMap = Collections.emptyMap();

        if (StringUtils.isNoneBlank(appConfigAsString)) {
            configMap = ToolsUtils.stringToMap(appConfigAsString, ",", "=");
        }
        setValue(APP_CONFIG, configMap);
        return this;
    }

    public CreateProject packageName(String packageName) {
        if (packageName == null) {
            return this;
        }
        packageName = CreateProjectHelper.checkPackageName(packageName);
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

    public CreateProject data(String dataAsString) {
        setValue(DATA, StringUtils.isNoneBlank(dataAsString) ? ToolsUtils.stringToMap(dataAsString, ",", "=")
                : Collections.emptyMap());

        return this;
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
        final SourceType sourceType = SourceType.resolve(extensions);
        setValue(CreateProjectKey.JAVA_VERSION, computeJavaVersion(sourceType, javaVersion));

        CreateProjectHelper.handleSpringConfiguration(values, extensions);

        setValue(EXTENSIONS, extensions);

        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, values);
        return new CreateProjectCommandHandler().execute(invocation);
    }
}
