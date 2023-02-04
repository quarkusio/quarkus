
package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.QuarkusGradleUtils.getSourceSet;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.gradle.extension.QuarkusPluginExtension;

public class QuarkusBuildConfiguration {
    static final String QUARKUS_BUILD_DIR = "quarkus-build";
    static final String QUARKUS_BUILD_GEN_DIR = QUARKUS_BUILD_DIR + "/gen";
    static final String QUARKUS_BUILD_APP_DIR = QUARKUS_BUILD_DIR + "/app";
    static final String QUARKUS_BUILD_DEP_DIR = QUARKUS_BUILD_DIR + "/dep";

    final Project project;
    final QuarkusPluginExtension extension;
    final SourceSet mainSourceSet;
    final FileCollection classpath;
    final ListProperty<String> forcedDependenciesProperty;
    final MapProperty<String, String> forcedPropertiesProperty;
    final List<String> ignoredEntries = new ArrayList<>();
    final Manifest manifest = new Manifest();

    private final EffectiveConfigHelper effectiveConfig;
    private boolean effective;
    private Map<String, String> configMap;

    public QuarkusBuildConfiguration(Project project, QuarkusPluginExtension extension) {
        this.project = project;
        this.extension = extension;

        mainSourceSet = getSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME);
        classpath = dependencyClasspath(mainSourceSet);

        effectiveConfig = new EffectiveConfigHelper();

        forcedDependenciesProperty = project.getObjects().listProperty(String.class);
        forcedPropertiesProperty = project.getObjects().mapProperty(String.class, String.class);
    }

    public void setForcedProperties(Map<String, String> forcedProperties) {
        forcedPropertiesProperty.putAll(forcedProperties);
    }

    Path genBuildDir() {
        return project.getBuildDir().toPath().resolve(QUARKUS_BUILD_GEN_DIR);
    }

    Path appBuildDir() {
        return project.getBuildDir().toPath().resolve(QUARKUS_BUILD_APP_DIR);
    }

    Path depBuildDir() {
        return project.getBuildDir().toPath().resolve(QUARKUS_BUILD_DEP_DIR);
    }

    /**
     * "final" location of the "fast-jar".
     */
    File fastJar() {
        return new File(project.getBuildDir(), outputDirectory());
    }

    /**
     * "final" location of the "uber-jar".
     */
    File runnerJar() {
        return new File(project.getBuildDir(), runnerJarFileName());
    }

    /**
     * "final" location of the "native" runner.
     */
    File nativeRunner() {
        return new File(project.getBuildDir(), nativeRunnerFileName());
    }

    String runnerJarFileName() {
        return String.format("%s.jar", runnerName());
    }

    String nativeRunnerFileName() {
        return runnerName();
    }

    String runnerName() {
        return extension.buildNativeRunnerName(Map.of());
    }

    String runnerBaseName() {
        return extension.buildNativeRunnerBaseName(Map.of());
    }

    String outputDirectory() {
        return effective().configMap().getOrDefault(QuarkusPlugin.OUTPUT_DIRECTORY,
                QuarkusPlugin.DEFAULT_OUTPUT_DIRECTORY);
    }

    String packageType() {
        return effective().configMap().getOrDefault(QuarkusPlugin.QUARKUS_PACKAGE_TYPE,
                QuarkusPlugin.DEFAULT_PACKAGE_TYPE);
    }

    Map<String, String> getQuarkusSystemProperties() {
        return effectiveConfig.quarkusSystemProperties;
    }

    Map<String, String> getQuarkusEnvProperties() {
        return effectiveConfig.quarkusEnvProperties;
    }

    static boolean isUberJarPackageType(String packageType) {
        // Layout:
        //   build/<runner-jar>
        return "uber-jar".equals(packageType);
    }

    static boolean isLegacyJarPackageType(String packageType) {
        // Layout:
        //   build/<runner-jar>
        //   build/lib/
        return "legacy-jar".equals(packageType);
    }

    static boolean isMutableJarPackageType(String packageType) {
        // like "fast-jar", but additional folder (not implemented, fallback to "full build" ATM).
        // Additional folder:
        //   build/<output-directory>/lib/deployment/
        //   ^ contains dependency jars AND generated files
        return "mutable-jar".equals(packageType);
    }

    static boolean isFastJarPackageType(String packageType) {
        // Layout:
        //   build/<output-directory>/<runner-jar>
        //   build/<output-directory>/lib/boot/
        //   build/<output-directory>/lib/main/
        //   build/<output-directory>/quarkus/
        //   build/<output-directory>/app/
        //   build/<output-directory>/...
        switch (packageType) {
            case "jar":
            case "fast-jar":
            case "native":
                return true;
            default:
                return false;
        }
    }

    QuarkusBuildConfiguration effective() {
        if (!effective) {
            configMap = buildEffectiveConfiguration();
            effective = true;
        }
        return this;
    }

    List<URL> applicationPropsSources() {
        return effectiveConfig.applicationPropertiesSourceUrls;
    }

    private Map<String, String> buildEffectiveConfiguration() {
        Map<String, String> map = effectiveConfig.applyForcedProperties(forcedPropertiesProperty.get())
                .applyBuildProperties(extension.getQuarkusBuildProperties().get())
                .applyProjectProperties(project.getProperties())
                .applyApplicationProperties(mainSourceSet.getResources().getSourceDirectories().getFiles(), project.getLogger())
                .buildEffectiveConfiguration();

        if (project.getLogger().isInfoEnabled()) {
            project.getLogger().info("Effective Quarkus application config: {}",
                    new TreeMap<>(map).entrySet().stream().map(Objects::toString)
                            .collect(Collectors.joining("\n    ", "\n    ", "")));
        }

        return map;
    }

    Map<String, String> configMap() {
        return configMap;
    }

    private static FileCollection dependencyClasspath(SourceSet mainSourceSet) {
        return mainSourceSet.getCompileClasspath().plus(mainSourceSet.getRuntimeClasspath())
                .plus(mainSourceSet.getAnnotationProcessorPath())
                .plus(mainSourceSet.getResources());
    }

}
