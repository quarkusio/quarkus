package io.quarkus.gradle.dsl;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import io.quarkus.gradle.extension.QuarkusPluginExtension;

/**
 * DSL extension for configuring Quarkus dev-mode
 *
 * @author Steve Ebersole
 */
public class DevMode {
    private final DirectoryProperty workingDirectory;

    @Inject
    public DevMode(Project project) {
        final SourceSet mainSourceSet = project.getExtensions().getByType(SourceSetContainer.class)
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        workingDirectory = project.getObjects().directoryProperty();
        workingDirectory.convention(
                // use the last directory in the source-set's compilation output
                // directories as the convention for the working directory.  this
                // is simply what the old code does
                project.getLayout().dir(project
                        .provider(() -> QuarkusPluginExtension.getLastFile(mainSourceSet.getOutput().getClassesDirs()))));
    }

    public DirectoryProperty getWorkingDirectory() {
        return workingDirectory;
    }
}
