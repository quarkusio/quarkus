package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.runtime.util.StringUtil;

public class QuarkusBuild extends QuarkusTask {

    private static final String NATIVE_PROPERTY_NAMESPACE = "quarkus.native";

    private List<String> ignoredEntries = new ArrayList<>();

    public QuarkusBuild() {
        super("Quarkus builds a runner jar based on the build jar");
    }

    public QuarkusBuild nativeArgs(Action<Map<String, ?>> action) {
        Map<String, ?> nativeArgsMap = new HashMap<>();
        action.execute(nativeArgsMap);
        for (Map.Entry<String, ?> nativeArg : nativeArgsMap.entrySet()) {
            System.setProperty(expandConfigurationKey(nativeArg.getKey()), nativeArg.getValue().toString());
        }
        return this;
    }

    @Optional
    @Input
    public List<String> getIgnoredEntries() {
        return ignoredEntries;
    }

    @Option(description = "When using the uber-jar option, this option can be used to "
            + "specify one or more entries that should be excluded from the final jar", option = "ignored-entry")
    public void setIgnoredEntries(List<String> ignoredEntries) {
        this.ignoredEntries.addAll(ignoredEntries);
    }

    @Classpath
    public FileCollection getClasspath() {
        SourceSet mainSourceSet = QuarkusGradleUtils.getSourceSet(getProject(), SourceSet.MAIN_SOURCE_SET_NAME);
        return mainSourceSet.getCompileClasspath().plus(mainSourceSet.getRuntimeClasspath())
                .plus(mainSourceSet.getAnnotationProcessorPath())
                .plus(mainSourceSet.getResources());
    }

    @Input
    public Map<Object, Object> getQuarkusBuildSystemProperties() {
        Map<Object, Object> quarkusSystemProperties = new HashMap<>();
        for (Map.Entry<Object, Object> systemProperty : System.getProperties().entrySet()) {
            if (systemProperty.getKey().toString().startsWith("quarkus.") &&
                    systemProperty.getValue() instanceof Serializable) {
                quarkusSystemProperties.put(systemProperty.getKey(), systemProperty.getValue());
            }
        }
        return quarkusSystemProperties;
    }

    @Input
    public Map<String, String> getQuarkusBuildEnvProperties() {
        Map<String, String> quarkusEnvProperties = new HashMap<>();
        for (Map.Entry<String, String> systemProperty : System.getenv().entrySet()) {
            if (systemProperty.getKey() != null && systemProperty.getKey().startsWith("QUARKUS_")) {
                quarkusEnvProperties.put(systemProperty.getKey(), systemProperty.getValue());
            }
        }
        return quarkusEnvProperties;
    }

    @OutputFile
    public File getRunnerJar() {
        return new File(getProject().getBuildDir(), extension().finalName() + "-runner.jar");
    }

    @OutputFile
    public File getNativeRunner() {
        return new File(getProject().getBuildDir(), extension().finalName() + "-runner");
    }

    @OutputDirectory
    public File getFastJar() {
        return new File(getProject().getBuildDir(), "quarkus-app");
    }

    @TaskAction
    public void buildQuarkus() {
        getLogger().lifecycle("building quarkus jar");

        final AppArtifact appArtifact = extension().getAppArtifact();
        appArtifact.setPaths(QuarkusGradleUtils.getOutputPaths(getProject()));
        final AppModelResolver modelResolver = extension().getAppModelResolver();

        final Properties effectiveProperties = getBuildSystemProperties(appArtifact);
        if (ignoredEntries != null && ignoredEntries.size() > 0) {
            String joinedEntries = String.join(",", ignoredEntries);
            effectiveProperties.setProperty("quarkus.package.user-configured-ignored-entries", joinedEntries);
        }
        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setAppModelResolver(modelResolver)
                .setTargetDirectory(getProject().getBuildDir().toPath())
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(effectiveProperties)
                .setAppArtifact(appArtifact)
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .build().bootstrap()) {

            // Processes launched from within the build task of Gradle (daemon) lose content
            // generated on STDOUT/STDERR by the process (see https://github.com/gradle/gradle/issues/13522).
            // We overcome this by letting build steps know that the STDOUT/STDERR should be explicitly
            // streamed, if they need to make available that generated data.
            // The io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled$Factory
            // does the necessary work to generate such a build item which the build step(s) can rely on
            appCreationContext.createAugmentor("io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled$Factory",
                    Collections.emptyMap()).createProductionApplication();

        } catch (BootstrapException e) {
            throw new GradleException("Failed to build a runnable JAR", e);
        }
    }

    private String expandConfigurationKey(String shortKey) {
        final String hyphenatedKey = StringUtil.hyphenate(shortKey);
        if (hyphenatedKey.startsWith(NATIVE_PROPERTY_NAMESPACE)) {
            return hyphenatedKey;
        }
        return String.format("%s.%s", NATIVE_PROPERTY_NAMESPACE, hyphenatedKey);
    }
}
