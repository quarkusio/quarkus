package io.quarkus.sbom;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ApplicationManifest {

    public static ApplicationManifest fromConfig(ApplicationManifestConfig config) {
        if (config.getDistributionDirectory() != null) {
            var builder = ApplicationManifestConfig.builder()
                    .setDistributionDirectory(config.getDistributionDirectory())
                    .setMainComponent(config.getMainComponent())
                    .setRunnerPath(config.getRunnerPath());
            for (var c : config.getComponents()) {
                builder.addComponent(c);
            }
            addRemainingContent(config, builder);
            config = builder.build();
        }
        var builder = ApplicationManifest.builder();
        builder.setMainComponent(config.getMainComponent())
                .setRunnerPath(config.getRunnerPath());
        for (var c : config.getComponents()) {
            builder.addComponent(c);
        }
        return builder.build();
    }

    private static void addRemainingContent(ApplicationManifestConfig config, ApplicationManifestConfig.Builder builder) {
        try {
            Files.walkFileTree(config.getDistributionDirectory(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    builder.addComponent(ApplicationComponent.builder().setPath(file));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ApplicationManifest {

        private Builder() {
            super();
        }

        private Builder(ApplicationManifest manifest) {
            super(manifest);
        }

        public Builder setMainComponent(ApplicationComponent main) {
            this.mainComponent = main;
            return this;
        }

        public Builder addComponent(ApplicationComponent component) {
            if (component == null) {
                throw new IllegalArgumentException("component is null");
            }
            if (components == null) {
                components = new ArrayList<>();
            }
            components.add(component);
            return this;
        }

        public Builder setRunnerPath(Path runnerPath) {
            this.runnerPath = runnerPath;
            return this;
        }

        public ApplicationManifest build() {
            return new ApplicationManifest(this);
        }
    }

    protected ApplicationComponent mainComponent;
    protected Collection<ApplicationComponent> components;
    protected Path runnerPath;

    private ApplicationManifest() {
    }

    private ApplicationManifest(ApplicationManifest builder) {
        if (builder.mainComponent == null) {
            throw new IllegalArgumentException("Main component is null");
        }
        this.mainComponent = builder.mainComponent.ensureImmutable();
        if (builder.components == null || builder.components.isEmpty()) {
            this.components = List.of();
        } else {
            var tmp = new ApplicationComponent[builder.components.size()];
            int i = 0;
            for (var c : builder.components) {
                tmp[i++] = c.ensureImmutable();
            }
            this.components = List.of(tmp);
        }
        this.runnerPath = builder.runnerPath;
    }

    public ApplicationComponent getMainComponent() {
        return mainComponent;
    }

    public Collection<ApplicationComponent> getComponents() {
        return components == null ? List.of() : components;
    }

    public Path getRunnerPath() {
        return runnerPath;
    }
}
