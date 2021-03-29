package io.quarkus.devtools.project;

import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.nio.file.Path;

public final class QuarkusProject {

    private final Path projectDirPath;
    private ExtensionCatalog catalog;
    private ResourceLoader resourceLoader;
    private final ExtensionManager extensionManager;

    private QuarkusProject(Path projectDirPath, ExtensionCatalog catalog, ResourceLoader codestartPathLoader,
            MessageWriter log,
            ExtensionManager extensionManager) {
        this.projectDirPath = requireNonNull(projectDirPath, "projectDirPath is required");
        this.catalog = requireNonNull(catalog, "catalog is required");
        this.resourceLoader = requireNonNull(codestartPathLoader, "resourceLoader is required");
        this.extensionManager = requireNonNull(extensionManager, "extensionManager is required");
    }

    public static QuarkusProject of(final Path projectDirPath, final ExtensionCatalog catalog,
            ResourceLoader resourceLoader, MessageWriter log,
            final ExtensionManager extensionManager) {
        return new QuarkusProject(projectDirPath, catalog, resourceLoader, log, extensionManager);
    }

    public static QuarkusProject of(Path projectDirPath, ExtensionCatalog catalog, ResourceLoader codestartsResourceLoader,
            MessageWriter log, BuildTool buildTool) {
        return new QuarkusProject(projectDirPath, catalog, codestartsResourceLoader, log,
                buildTool.createExtensionManager(projectDirPath, catalog));
    }

    public Path getProjectDirPath() {
        return projectDirPath;
    }

    public BuildTool getBuildTool() {
        return extensionManager.getBuildTool();
    }

    public ExtensionManager getExtensionManager() {
        return extensionManager;
    }

    public ExtensionCatalog getExtensionsCatalog() {
        return catalog;
    }

    public ResourceLoader getCodestartsResourceLoader() {
        return resourceLoader;
    }

    public static BuildTool resolveExistingProjectBuildTool(Path projectDirPath) {
        if (projectDirPath.resolve("pom.xml").toFile().exists()) {
            return BuildTool.MAVEN;
        } else if (projectDirPath.resolve("build.gradle").toFile().exists()) {
            return BuildTool.GRADLE;
        } else if (projectDirPath.resolve("build.gradle.kts").toFile().exists()) {
            return BuildTool.GRADLE_KOTLIN_DSL;
        }
        return null;
    }

}
