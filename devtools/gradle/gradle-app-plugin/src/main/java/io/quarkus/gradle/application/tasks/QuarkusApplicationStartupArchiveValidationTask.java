package io.quarkus.gradle.application.tasks;

import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;

/**
 * Generated implementation task that validates a startup archive produced by integration-test training.
 * <p>
 * Public visibility is required for Gradle decoration. The task owns no output; it verifies that exactly the
 * file-system shape required by the archive type is present and non-empty. It is not a supported typed user entry point
 * and makes no compatibility commitment for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Startup archive validation is cheap to rerun and owns no output")
public abstract class QuarkusApplicationStartupArchiveValidationTask extends DefaultTask {

    /**
     * Returns the concrete archive type and required file-system shape.
     *
     * @return the archive type
     */
    @Input
    public abstract Property<QuarkusApplicationJvmStartupArchiveType> getArchiveType();

    /**
     * Returns the archive file for file-shaped types.
     *
     * @return the optional archive file
     */
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getArchiveFile();

    /**
     * Returns the archive directory for directory-shaped types.
     *
     * @return the optional archive directory
     */
    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getArchiveDirectory();

    /**
     * Verifies that the selected type has exactly one matching, non-empty archive file or directory.
     */
    @TaskAction
    public void validateStartupArchive() {
        QuarkusApplicationJvmStartupArchiveType type = getArchiveType().get();
        Path archive;
        if (type.isDirectory()) {
            if (getArchiveFile().isPresent() || !getArchiveDirectory().isPresent()) {
                throw new GradleException("Startup archive type " + type + " requires one directory and no file");
            }
            archive = getArchiveDirectory().get().getAsFile().toPath();
            try (var children = Files.list(archive)) {
                if (children.findAny().isEmpty()) {
                    throw new GradleException("Startup archive directory " + archive + " is empty");
                }
            } catch (java.io.IOException e) {
                throw new GradleException("Failed to inspect startup archive directory " + archive, e);
            }
        } else {
            if (getArchiveDirectory().isPresent() || !getArchiveFile().isPresent()) {
                throw new GradleException("Startup archive type " + type + " requires one file and no directory");
            }
            archive = getArchiveFile().get().getAsFile().toPath();
            if (!Files.isRegularFile(archive)) {
                throw new GradleException("Startup archive file " + archive + " does not exist");
            }
            try {
                if (Files.size(archive) == 0) {
                    throw new GradleException("Startup archive file " + archive + " is empty");
                }
            } catch (java.io.IOException e) {
                throw new GradleException("Failed to inspect startup archive file " + archive, e);
            }
        }
    }
}
