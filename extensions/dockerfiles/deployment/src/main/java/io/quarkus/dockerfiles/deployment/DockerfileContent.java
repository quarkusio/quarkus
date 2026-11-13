package io.quarkus.dockerfiles.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.dockerfiles.spi.Distribution;
import io.quarkus.dockerfiles.spi.DockerfileDependencyBuildItem;
import io.quarkus.dockerfiles.spi.DockerfileKind;
import io.quarkus.qute.Qute;
import io.quarkus.qute.Qute.Fmt;

public class DockerfileContent {

    public static final String FROM = "from";
    public static final String TYPE = "type";
    public static final String APPLICATION_NAME = "application-name";
    public static final String OUTPUT_DIR = "output-dir";
    public static final String DEPENDENCIES = "dependencies";
    public static final String BUILDTOOL = "buildtool";
    public static final String PROJECT = "project";

    public interface FromStep {
        ConfigurationStep from(String from);
    }

    public interface ConfigurationStep {
        ConfigurationStep applicationName(String applicationName);

        ConfigurationStep outputDir(Path outputDir);

        ConfigurationStep outputDir(String outputDir);

        ConfigurationStep buildTool(BuildTool buildTool);

        ConfigurationStep dependencies(DockerfileDependencyBuildItem... dependencies);

        ConfigurationStep dependencies(List<DockerfileDependencyBuildItem> dependencies);

        ConfigurationStep applicableDependencies(DockerfileDependencyBuildItem... dependencies);

        ConfigurationStep applicableDependencies(List<DockerfileDependencyBuildItem> dependencies);

        String build();
    }

    private static class Builder implements FromStep, ConfigurationStep {
        private final String type;
        private final DockerfileKind kind;
        private final String template;

        private String from;
        private Distribution distribution;
        private String applicationName;
        private String outputDir;
        private BuildTool buildTool = BuildTool.MAVEN; // Default to Maven
        private List<DockerfileDependencyBuildItem> dependencies = List.of();

        private Builder(String type, DockerfileKind kind, String template) {
            this.type = type;
            this.kind = kind;
            this.template = template;
        }

        @Override
        public ConfigurationStep from(String from) {
            this.from = Objects.requireNonNull(from, "from must not be null");
            this.distribution = DistributionDetector.detectDistribution(from);
            return this;
        }

        @Override
        public ConfigurationStep applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        @Override
        public ConfigurationStep outputDir(Path outputDir) {
            this.outputDir = outputDir != null ? outputDir.toString() : null;
            return this;
        }

        @Override
        public ConfigurationStep outputDir(String outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        @Override
        public ConfigurationStep buildTool(BuildTool buildTool) {
            this.buildTool = buildTool != null ? buildTool : BuildTool.MAVEN;
            return this;
        }

        @Override
        public ConfigurationStep dependencies(DockerfileDependencyBuildItem... dependencies) {
            return dependencies(List.of(dependencies));
        }

        @Override
        public ConfigurationStep dependencies(List<DockerfileDependencyBuildItem> dependencies) {
            if (dependencies == null || dependencies.isEmpty()) {
                this.dependencies = List.of();
                return this;
            }

            // Validate that all dependencies are applicable to the detected distribution
            validateDependenciesForDistribution(dependencies, distribution, kind);

            this.dependencies = dependencies;
            return this;
        }

        @Override
        public ConfigurationStep applicableDependencies(DockerfileDependencyBuildItem... dependencies) {
            return applicableDependencies(List.of(dependencies));
        }

        @Override
        public ConfigurationStep applicableDependencies(List<DockerfileDependencyBuildItem> dependencies) {
            if (dependencies == null || dependencies.isEmpty()) {
                this.dependencies = List.of();
                return this;
            }

            // Filter dependencies that are applicable to the detected distribution and kind
            List<DockerfileDependencyBuildItem> filteredDependencies = dependencies.stream()
                    .filter(dep -> dep.appliesTo(kind, distribution))
                    .collect(Collectors.toList());

            // Delegate to the strict dependencies method
            return dependencies(filteredDependencies);
        }

        private void validateDependenciesForDistribution(List<DockerfileDependencyBuildItem> dependencies,
                Distribution distribution, DockerfileKind kind) {
            if (distribution == Distribution.UNKNOWN) {
                return; // Skip validation for unknown distributions
            }

            for (DockerfileDependencyBuildItem dependency : dependencies) {
                if (!dependency.appliesTo(kind, distribution)) {
                    throw new IllegalArgumentException(
                            String.format("Dependency %s does not apply to distribution %s and kind %s. " +
                                    "Available distributions for this dependency: %s",
                                    dependency, distribution, kind, dependency.getPackageNames().keySet()));
                }
            }
        }

        @Override
        public String build() {
            String dependenciesString = generateDependenciesString(dependencies, distribution);

            Map<String, Object> buildToolData = Map.of(
                    "cmd", buildTool == BuildTool.MAVEN ? "mvn" : "gradle",
                    "build-dir", buildTool.getBuildDirectory());

            Map<String, Object> data = Map.of(
                    FROM, from != null ? from : "",
                    TYPE, type,
                    APPLICATION_NAME, applicationName != null ? applicationName : "",
                    OUTPUT_DIR, outputDir != null ? outputDir : "",
                    DEPENDENCIES, dependenciesString,
                    BUILDTOOL, buildToolData);

            Fmt fmt = Qute.fmt(readResource(template));
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                fmt = fmt.data(entry.getKey(), entry.getValue());
            }
            return fmt.render();
        }
    }

    public static FromStep jvmBuilder() {
        return new Builder("jvm", DockerfileKind.JVM, "Dockerfile.tpl.qute.jvm");
    }

    public static FromStep nativeBuilder() {
        return new Builder("native", DockerfileKind.NATIVE, "Dockerfile.tpl.qute.native");
    }

    private static String generateDependenciesString(List<DockerfileDependencyBuildItem> dependencies,
            Distribution distribution) {
        List<String> packageNames = dependencies.stream()
                .map(dep -> dep.getPackageNameFor(distribution))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toList());

        if (packageNames.isEmpty()) {
            return "";
        }

        // Generate the installation command based on the distribution
        String packagesString = String.join(" ", packageNames);
        return distribution.generateInstallCommand(packagesString);
    }

    private static String readResource(String resource) {
        try (InputStream in = DockerfileContent.class.getClassLoader().getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), Charset.defaultCharset());
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}
