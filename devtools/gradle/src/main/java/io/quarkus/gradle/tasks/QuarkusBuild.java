package io.quarkus.gradle.tasks;

import java.io.File;
import java.util.ArrayList;
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

    private boolean uberJar;

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

    @Input
    public boolean isUberJar() {
        return uberJar;
    }

    @Option(description = "Set to true if the build task should build an uberjar", option = "uber-jar")
    public void setUberJar(boolean uberJar) {
        this.uberJar = uberJar;
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

    @OutputFile
    public File getOutputDir() {
        return new File(getProject().getBuildDir(), extension().finalName() + "-runner.jar");
    }

    @TaskAction
    public void buildQuarkus() {
        getLogger().lifecycle("building quarkus runner");

        final AppArtifact appArtifact = extension().getAppArtifact();
        appArtifact.setPaths(QuarkusGradleUtils.getOutputPaths(getProject()));
        final AppModelResolver modelResolver = extension().getAppModelResolver();

        final Properties realProperties = getBuildSystemProperties(appArtifact);

        boolean clear = false;
        if (uberJar && System.getProperty("quarkus.package.uber-jar") == null) {
            System.setProperty("quarkus.package.uber-jar", "true");
            clear = true;
        }
        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setAppModelResolver(modelResolver)
                .setTargetDirectory(getProject().getBuildDir().toPath())
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(realProperties)
                .setAppArtifact(appArtifact)
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .build().bootstrap()) {

            appCreationContext.createAugmentor().createProductionApplication();

        } catch (BootstrapException e) {
            throw new GradleException("Failed to build a runnable JAR", e);
        } finally {
            if (clear) {
                System.clearProperty("quarkus.package.uber-jar");
            }
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
