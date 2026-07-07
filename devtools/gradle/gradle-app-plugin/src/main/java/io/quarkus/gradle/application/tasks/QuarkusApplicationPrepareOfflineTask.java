package io.quarkus.gradle.application.tasks;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.model.pom.PomClosureResultCodec;

/**
 * Resolves dependency artifacts and POM closures needed for later offline application-plugin work.
 * <p>
 * The plugin registers the aggregate {@code quarkusApplicationPrepareOffline} task and internal per-build preparation
 * tasks. The aggregate includes the standard application scopes and the named builds that opt in through their DSL.
 * Execution populates Gradle's dependency cache, reports the prepared scopes, and warns when a POM closure remains
 * incomplete. It has no portable output and is therefore not build-cacheable.
 * <p>
 * The supported compatibility contract covers the plugin-registered aggregate task and its documented behavior. No
 * compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "The task populates Gradle's dependency cache and owns no portable output")
public abstract class QuarkusApplicationPrepareOfflineTask extends DefaultTask {

    /**
     * Returns the dependency artifacts resolved to populate Gradle's cache.
     *
     * @return the dependency files
     */
    @Classpath
    public abstract ConfigurableFileCollection getDependencyFiles();

    /**
     * Returns generated POM-closure result files inspected for unresolved metadata.
     *
     * @return the POM-closure result files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getPomClosureFiles();

    /**
     * Returns human-readable scope names reported in the task's lifecycle message.
     *
     * @return the preparation scopes
     */
    @Input
    public abstract ListProperty<String> getPreparationScopes();

    /**
     * Resolves the configured files and reports whether their POM closures are complete.
     */
    @TaskAction
    public void prepareOffline() {
        Set<File> dependencyFiles = getDependencyFiles().getFiles();
        Set<File> pomClosureFiles = getPomClosureFiles().getFiles();
        List<String> scopes = getPreparationScopes().get();
        int unresolvedPoms = unresolvedPomCount(pomClosureFiles);
        getLogger().lifecycle("Prepared {} dependency artifacts and inspected {} POM closure result files{}.",
                dependencyFiles.size(), pomClosureFiles.size(),
                scopes.isEmpty() ? "" : " (" + String.join(", ", scopes) + ")");
        if (unresolvedPoms > 0) {
            getLogger().warn("Offline preparation is incomplete: found {} unresolved POM entries. "
                    + "Parent/imported-BOM metadata may be incomplete while offline.", unresolvedPoms);
        }
    }

    private static int unresolvedPomCount(Set<File> pomClosureFiles) {
        try {
            int unresolved = 0;
            for (File pomClosureFile : pomClosureFiles) {
                unresolved += PomClosureResultCodec.read(pomClosureFile.toPath()).missingPoms().size();
            }
            return unresolved;
        } catch (IOException | IllegalArgumentException e) {
            throw new GradleException("Failed to inspect a generated POM closure", e);
        }
    }
}
