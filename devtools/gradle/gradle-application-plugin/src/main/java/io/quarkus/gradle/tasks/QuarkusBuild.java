package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.runtime.util.StringUtil;

public class QuarkusBuild extends QuarkusTask {

    private static final String NATIVE_PROPERTY_NAMESPACE = "quarkus.native";
    private static final String MANIFEST_SECTIONS_PROPERTY_PREFIX = "quarkus.package.manifest.manifest-sections";
    private static final String MANIFEST_ATTRIBUTES_PROPERTY_PREFIX = "quarkus.package.manifest.attributes";

    private final Manifest manifest = new Manifest();
    private final ListProperty<String> ignoredEntries;
    private final Provider<RegularFile> runnerJar;
    private final Provider<RegularFile> nativeRunner;
    private final Provider<RegularFile> fastJar;

    public QuarkusBuild() {
        super("Quarkus builds a runner jar based on the build jar");

        ignoredEntries = getProject().getObjects().listProperty(String.class);

        runnerJar = getProject().getLayout().getBuildDirectory().file(runnerJarName());
        nativeRunner = getProject().getLayout().getBuildDirectory().file(nativeRunnerName());
        fastJar = getProject().getLayout().getBuildDirectory().file("quarkus-app");
    }

    private Provider<String> runnerJarName() {
        return extension().getFinalName().map((finalName) -> finalName + "-runner.jar");
    }

    private Provider<String> nativeRunnerName() {
        return extension().getFinalName().map((finalName) -> finalName + "-runner");
    }

    public QuarkusBuild nativeArgs(Action<Map<String, ?>> action) {
        Map<String, ?> nativeArgsMap = new HashMap<>();
        action.execute(nativeArgsMap);
        for (Map.Entry<String, ?> nativeArg : nativeArgsMap.entrySet()) {
            System.setProperty(expandConfigurationKey(nativeArg.getKey()), nativeArg.getValue().toString());
        }
        return this;
    }

    @Input
    @Optional
    @Option(description = "When using the uber-jar option, this option can be used to "
            + "specify one or more entries that should be excluded from the final jar", option = "ignored-entry")
    public ListProperty<String> getIgnoredEntriesList() {
        return ignoredEntries;
    }

    @Internal
    public List<String> getIgnoredEntries() {
        return ignoredEntries.get();
    }

    public void setIgnoredEntries(List<String> values) {
        ignoredEntries.addAll(values);
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

    @Internal
    public Manifest getManifest() {
        return this.manifest;
    }

    public QuarkusBuild manifest(Action<Manifest> action) {
        action.execute(this.getManifest());
        return this;
    }

    @OutputFile
    public Provider<RegularFile> getRunnerJarFile() {
        return runnerJar;
    }

    /**
     * Same as {@link #getRunnerJarFile()}, except this method resolves the value.
     * <p>
     * Prefer {@link #getRunnerJarFile()} for delayed configuration
     */
    @Internal
    public File getRunnerJar() {
        return getRunnerJarFile().get().getAsFile();
    }

    @OutputFile
    public Provider<RegularFile> getNativeRunnerFile() {
        return nativeRunner;
    }

    /**
     * Same as {@link #getNativeRunnerFile()}, except this method resolves the value.
     * <p>
     * Prefer {@link #getNativeRunnerFile()} for delayed configuration
     */
    @Internal
    public File getNativeRunner() {
        return getNativeRunnerFile().get().getAsFile();
    }

    @OutputDirectory
    public Provider<RegularFile> getFastJarFile() {
        return fastJar;
    }

    /**
     * Same as {@link #getFastJarFile()}, except this method resolves the value.
     * <p>
     * Prefer {@link #getFastJarFile()} for delayed configuration
     */
    @Internal
    public File getFastJar() {
        return getFastJarFile().get().getAsFile();
    }

    @TaskAction
    public void buildQuarkus() {
        final ApplicationModel appModel;
        try {
            appModel = extension().getAppModelResolver().resolveModel(new GACTV(getProject().getGroup().toString(),
                    getProject().getName(), getProject().getVersion().toString()));
        } catch (AppModelResolverException e) {
            throw new GradleException("Failed to resolve Quarkus application model for " + getProject().getPath(), e);
        }

        final Properties effectiveProperties = getBuildSystemProperties(appModel.getAppArtifact());
        if (!ignoredEntries.get().isEmpty()) {
            String joinedEntries = String.join(",", ignoredEntries.get());
            effectiveProperties.setProperty("quarkus.package.user-configured-ignored-entries", joinedEntries);
        }

        exportCustomManifestProperties(effectiveProperties);

        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setExistingModel(appModel)
                .setTargetDirectory(getProject().getBuildDir().toPath())
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(effectiveProperties)
                .setAppArtifact(appModel.getAppArtifact())
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

    private void exportCustomManifestProperties(Properties buildSystemProperties) {
        if (this.manifest == null) {
            return;
        }

        for (Map.Entry<String, Object> attribute : manifest.getAttributes().entrySet()) {
            buildSystemProperties.put(toManifestAttributeKey(attribute.getKey()),
                    attribute.getValue());
        }

        for (Map.Entry<String, Attributes> section : manifest.getSections().entrySet()) {
            for (Map.Entry<String, Object> attribute : section.getValue().entrySet()) {
                buildSystemProperties
                        .put(toManifestSectionAttributeKey(section.getKey(), attribute.getKey()), attribute.getValue());
            }
        }
    }

    private String toManifestAttributeKey(String key) {
        if (key.contains("\"")) {
            throw new GradleException("Manifest entry name " + key + " is invalid. \" characters are not allowed.");
        }
        return String.format("%s.\"%s\"", MANIFEST_ATTRIBUTES_PROPERTY_PREFIX, key);
    }

    private String toManifestSectionAttributeKey(String section, String key) {
        if (section.contains("\"")) {
            throw new GradleException("Manifest section name " + section + " is invalid. \" characters are not allowed.");
        }
        if (key.contains("\"")) {
            throw new GradleException("Manifest entry name " + key + " is invalid. \" characters are not allowed.");
        }
        return String.format("%s.\"%s\".\"%s\"", MANIFEST_SECTIONS_PROPERTY_PREFIX, section,
                key);
    }

    private String expandConfigurationKey(String shortKey) {
        final String hyphenatedKey = StringUtil.hyphenate(shortKey);
        if (hyphenatedKey.startsWith(NATIVE_PROPERTY_NAMESPACE)) {
            return hyphenatedKey;
        }
        return String.format("%s.%s", NATIVE_PROPERTY_NAMESPACE, hyphenatedKey);
    }
}
