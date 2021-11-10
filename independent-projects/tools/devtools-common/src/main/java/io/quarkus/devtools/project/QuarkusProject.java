package io.quarkus.devtools.project;

import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.nio.file.Path;
import java.util.List;

public final class QuarkusProject {

    private final Path projectDirPath;
    private ExtensionCatalog catalog;
    private List<ResourceLoader> codestartResourceLoaders;
    private final ExtensionManager extensionManager;
    private final MessageWriter log;

    private QuarkusProject(Builder builder) {
        this.projectDirPath = builder.projectDirPath;
        this.catalog = builder.catalog;
        this.codestartResourceLoaders = builder.resourceLoaders;
        this.extensionManager = builder.extensionManager;
        this.log = builder.log;
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

    public List<ResourceLoader> getCodestartResourceLoaders() {
        return codestartResourceLoaders;
    }

    public MessageWriter log() {
        return log;
    }

    @Deprecated
    public static BuildTool resolveExistingProjectBuildTool(Path projectDirPath) {
        return BuildTool.fromProject(projectDirPath);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Path projectDirPath;
        private BuildTool buildTool;
        private ExtensionCatalog catalog;
        private List<ResourceLoader> resourceLoaders;
        private ExtensionManager extensionManager;
        private MessageWriter log;

        public Builder projectDir(Path projectDirPath) {
            this.projectDirPath = projectDirPath;
            return this;
        }

        public Builder extensionCatalog(ExtensionCatalog catalog) {
            this.catalog = catalog;
            return this;
        }

        public Builder codestartResourceLoaders(List<ResourceLoader> codestartResourceLoaders) {
            this.resourceLoaders = codestartResourceLoaders;
            return this;
        }

        public Builder buildTool(BuildTool buildTool) {
            this.buildTool = buildTool;
            return this;
        }

        public Builder extensionManager(ExtensionManager extensionManager) {
            this.extensionManager = extensionManager;
            return this;
        }

        public Builder log(MessageWriter log) {
            this.log = log;
            return this;
        }

        public QuarkusProject build() {
            this.log = (log == null ? MessageWriter.info() : log);
            requireNonNull(projectDirPath, "projectDirPath is required");
            requireNonNull(resourceLoaders, "List of ResourceLoaders is required");
            requireNonNull(catalog, "extension catalog is required");

            if (extensionManager == null) {
                if (buildTool != null) {
                    extensionManager = buildTool.createExtensionManager(projectDirPath, catalog);
                }
                requireNonNull(extensionManager, "extensionManager is required");
            } else if (buildTool != null && extensionManager.getBuildTool() != buildTool) {
                extensionManager = buildTool.createExtensionManager(projectDirPath, catalog);
            }

            return new QuarkusProject(this);
        }
    }

}
