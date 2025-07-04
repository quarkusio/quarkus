package io.quarkus.gradle.actions;

import static io.quarkus.gradle.extension.QuarkusPluginExtension.getLastFile;
import static io.quarkus.runtime.LaunchMode.TEST;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.tasks.EffectiveConfigProvider;
import io.quarkus.gradle.tasks.QuarkusPluginExtensionView;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.smallrye.config.SmallRyeConfig;

public class BeforeTestAction implements Action<Task> {

    private final File projectDir;
    private final FileCollection combinedOutputSourceDirs;
    private final Provider<RegularFile> applicationModelPath;
    private final Provider<File> nativeRunnerPath;
    private final FileCollection mainSourceSetClassesDir;
    private final QuarkusPluginExtensionView extensionView;
    private final MapProperty<String, Object> manifestAttributes;
    private final MapProperty<String, Attributes> manifestSections;

    public BeforeTestAction(File projectDir, FileCollection combinedOutputSourceDirs,
            Provider<RegularFile> applicationModelPath, Provider<File> nativeRunnerPath,
            FileCollection mainSourceSetClassesDir,
            QuarkusPluginExtensionView extensionView,
            MapProperty<String, Object> manifestAttributes,
            MapProperty<String, Attributes> manifestSections) {
        this.projectDir = projectDir;
        this.combinedOutputSourceDirs = combinedOutputSourceDirs;
        this.applicationModelPath = applicationModelPath;
        this.nativeRunnerPath = nativeRunnerPath;
        this.mainSourceSetClassesDir = mainSourceSetClassesDir;
        this.extensionView = extensionView;
        this.manifestAttributes = manifestAttributes;
        this.manifestSections = manifestSections;

    }

    @Override
    public void execute(Task t) {
        try {
            Test task = (Test) t;
            final Map<String, Object> props = task.getSystemProperties();

            ApplicationModel applicationModel = ToolingUtils
                    .deserializeAppModel(applicationModelPath.get().getAsFile().toPath());

            SmallRyeConfig config = effectiveProvider().buildEffectiveConfiguration(applicationModel, new HashMap<>())
                    .getConfig();
            config.getOptionalValue(TEST.getProfileKey(), String.class)
                    .ifPresent(value -> props.put(TEST.getProfileKey(), value));

            final Path serializedModel = applicationModelPath.get().getAsFile().toPath();
            props.put(BootstrapConstants.SERIALIZED_TEST_APP_MODEL, serializedModel.toString());

            StringJoiner outputSourcesDir = new StringJoiner(",");
            for (File outputSourceDir : combinedOutputSourceDirs.getFiles()) {
                outputSourcesDir.add(outputSourceDir.getAbsolutePath());
            }
            props.put(BootstrapConstants.OUTPUT_SOURCES_DIR, outputSourcesDir.toString());

            final File outputDirectoryAsFile = getLastFile(mainSourceSetClassesDir);

            Path projectDirPath = projectDir.toPath();

            // Identify the folder containing the sources associated with this test task
            String fileList = task.getTestClassesDirs().getFiles().stream()
                    .filter(File::exists)
                    .distinct()
                    .map(testSrcDir -> String.format("%s:%s",
                            projectDirPath.relativize(testSrcDir.toPath()),
                            projectDirPath.relativize(outputDirectoryAsFile.toPath())))
                    .collect(Collectors.joining(","));
            task.environment(BootstrapConstants.TEST_TO_MAIN_MAPPINGS, fileList);
            task.getLogger().debug("test dir mapping - {}", fileList);

            String nativeRunner = nativeRunnerPath.get().toPath().toAbsolutePath().toString();
            props.put("native.image.path", nativeRunner);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve deployment classpath", e);
        }
    }

    private EffectiveConfigProvider effectiveProvider() {
        return new EffectiveConfigProvider(
                extensionView.getIgnoredEntries(),
                extensionView.getMainResources(),
                extensionView.getForcedProperties(),
                extensionView.getProjectProperties(),
                extensionView.getQuarkusBuildProperties(),
                extensionView.getQuarkusRelevantProjectProperties(),
                manifestAttributes,
                manifestSections,
                extensionView.getNativeBuild(),
                extensionView.getQuarkusProfileSystemVariable(),
                extensionView.getQuarkusProfileEnvVariable());
    }
}
