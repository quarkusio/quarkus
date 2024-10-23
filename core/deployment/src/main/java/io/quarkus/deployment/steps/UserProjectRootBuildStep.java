package io.quarkus.deployment.steps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.UserProjectRootBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class UserProjectRootBuildStep {

    /**
     * Resolves the project directories based on the output target {@link OutputTargetBuildItem}.
     *
     * @return a {@link UserProjectRootBuildItem} build item containing the resolved project
     *         root as {@link java.nio.file.Path}.
     */
    @BuildStep
    UserProjectRootBuildItem rootDir(final OutputTargetBuildItem outputTarget) {
        Path projectRoot = findProjectRoot(outputTarget.getOutputDirectory());
        if (projectRoot != null) {
            return new UserProjectRootBuildItem(projectRoot);
        }
        return null;
    }

    private static Path findProjectRoot(Path outputDirectory) {
        Path currentPath = outputDirectory;
        do {
            if (Files.exists(currentPath.resolve(Paths.get("src", "main")))
                    || Files.exists(currentPath.resolve(Paths.get("config", "application.properties")))
                    || Files.exists(currentPath.resolve(Paths.get("config", "application.yaml")))
                    || Files.exists(currentPath.resolve(Paths.get("config", "application.yml")))) {
                return currentPath.normalize();
            }
            if (currentPath.getParent() != null && Files.exists(currentPath.getParent())) {
                currentPath = currentPath.getParent();
            } else {
                return null;
            }
        } while (true);
    }
}
