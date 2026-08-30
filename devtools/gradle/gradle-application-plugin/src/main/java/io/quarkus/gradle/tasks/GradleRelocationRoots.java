package io.quarkus.gradle.tasks;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;

import io.quarkus.bootstrap.app.ApplicationModelRelocation;

/**
 * The roots a Gradle build expresses the serialized application model's absolute paths relative to,
 * so that identical inputs built from different checkouts serialize to identical bytes.
 * <p>
 * The environment roots - the local Maven repository and the Gradle home - are derived by any reader
 * on its own; the build, project and root directories are only known to the build. The build directory
 * is listed separately from the project directory because it can be configured to sit outside it.
 * <p>
 * Builds included through {@code includeBuild(...)} get no root: their location is not recoverable by
 * a reader that has only the model's path, so their artifacts stay absolute.
 * <p>
 * This lives in one place because both the task that writes the model and the tasks that read it back
 * have to agree on the set of roots: a root the writer uses and a reader does not know leaves an
 * unresolved token.
 */
final class GradleRelocationRoots {

    private GradleRelocationRoots() {
    }

    /**
     * @param layout the layout supplying the project and build directories
     * @param gradleUserHome the Gradle home, when the build knows it
     * @param rootDirectory the root directory of the build, when the build knows it
     * @return the roots to relocate against, including those every reader derives from its environment
     */
    static List<ApplicationModelRelocation.Root> of(ProjectLayout layout, DirectoryProperty gradleUserHome,
            DirectoryProperty rootDirectory) {
        final List<ApplicationModelRelocation.Root> roots = new ArrayList<>();
        roots.add(new ApplicationModelRelocation.Root(ApplicationModelRelocation.BUILD_DIR_ROOT,
                layout.getBuildDirectory().get().getAsFile().toPath()));
        roots.add(new ApplicationModelRelocation.Root(ApplicationModelRelocation.PROJECT_DIR_ROOT,
                layout.getProjectDirectory().getAsFile().toPath()));
        if (gradleUserHome.isPresent()) {
            // Gradle's own value, which beats the guess environmentRoots() makes from the environment
            roots.add(new ApplicationModelRelocation.Root(ApplicationModelRelocation.GRADLE_USER_HOME_ROOT,
                    gradleUserHome.get().getAsFile().toPath()));
        }
        if (rootDirectory.isPresent()) {
            roots.add(new ApplicationModelRelocation.Root(ApplicationModelRelocation.ROOT_DIR_ROOT,
                    rootDirectory.get().getAsFile().toPath()));
        }
        return ApplicationModelRelocation.withEnvironmentRoots(roots);
    }
}
