package io.quarkus.maven.extension.deployment.metadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import io.quarkus.annotation.processor.documentation.config.formatter.JavadocTransformer;
import io.quarkus.annotation.processor.documentation.config.merger.JavadocMerger;
import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.merger.MergedModel;
import io.quarkus.annotation.processor.documentation.config.merger.MergedModel.ConfigRootKey;
import io.quarkus.annotation.processor.documentation.config.merger.ModelMerger;
import io.quarkus.annotation.processor.documentation.config.model.AbstractConfigItem;
import io.quarkus.annotation.processor.documentation.config.model.ConfigItemVisitor;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty.PropertyPath;
import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.Extension.NameSource;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;
import io.quarkus.annotation.processor.documentation.config.model.SourceElementType;
import io.quarkus.annotation.processor.documentation.config.util.JacksonMappers;
import io.quarkus.maven.extension.deployment.metadata.model.spring.QuarkusConfigAdditionalMetadataProperty;
import io.quarkus.maven.extension.deployment.metadata.model.spring.QuarkusConfigAdditionalMetadataProperty.ConfigPhase;
import io.quarkus.maven.extension.deployment.metadata.model.spring.SpringConfigMetadataDeprecation;
import io.quarkus.maven.extension.deployment.metadata.model.spring.SpringConfigMetadataGroup;
import io.quarkus.maven.extension.deployment.metadata.model.spring.SpringConfigMetadataHint;
import io.quarkus.maven.extension.deployment.metadata.model.spring.SpringConfigMetadataHintValue;
import io.quarkus.maven.extension.deployment.metadata.model.spring.SpringConfigMetadataModel;
import io.quarkus.maven.extension.deployment.metadata.model.spring.SpringConfigMetadataProperty;

@Mojo(name = "attach-config-metadata", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class AttachConfigMetadataMojo extends AbstractMojo {

    private static final String DEPLOYMENT_ARTIFACT_SUFFIX = "-deployment";
    private static final String META_INF = "META-INF";
    private static final Path OUTPUT_FILE = Path.of("config-metadata.json");

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    protected File targetDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    protected File outputDirectory;

    @Parameter(defaultValue = "false")
    private boolean skip;

    private final List<FileSystem> openZipFileSystems = new ArrayList<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }

        try {
            List<Path> metaInfDirectories = getMetaInfDirectories();
            SpringConfigMetadataModel springConfigMetadataModel = generateSpringConfigMetadataModel(metaInfDirectories);

            Path springConfigMetadataModelPath = targetDirectory.toPath().resolve(OUTPUT_FILE);
            JacksonMappers.jsonObjectWriter().writeValue(springConfigMetadataModelPath.toFile(), springConfigMetadataModel);

            // attach the metadata to the build
            // note that we probably want to attach a zip/jar containing all sorts of metadata rather than a single file
            projectHelper.attachArtifact(project, "json", "config-metadata", springConfigMetadataModelPath.toFile());
        } catch (Exception e) {
            getLog().error("Unable to generate the metadata", e);
        } finally {
            for (FileSystem openZipFileSystem : openZipFileSystems) {
                try {
                    openZipFileSystem.close();
                } catch (IOException e) {
                    getLog().error("Unable to close jar file system", e);
                }
            }
        }
    }

    private SpringConfigMetadataModel generateSpringConfigMetadataModel(List<Path> metaInfDirectories) {
        MergedModel mergedModel = ModelMerger.mergeModel(metaInfDirectories);
        if (mergedModel.isEmpty()) {
            return SpringConfigMetadataModel.empty();
        }

        Map<ConfigRootKey, ConfigRoot> configRoots = mergedModel.getConfigRoots().get(getExtension());
        if (configRoots == null || configRoots.isEmpty()) {
            return SpringConfigMetadataModel.empty();
        }

        JavadocRepository javadocRepository = JavadocMerger.mergeJavadocElements(metaInfDirectories);

        // TODO: for now we don't generate the groups, we will see how it goes
        List<SpringConfigMetadataGroup> groups = new ArrayList<>();
        List<SpringConfigMetadataProperty> properties = new ArrayList<>();
        List<SpringConfigMetadataHint> hints = new ArrayList<>();

        for (ConfigRoot configRoot : configRoots.values()) {
            configRoot.walk(new ConfigItemVisitor() {

                @Override
                public void visit(AbstractConfigItem configItem) {
                    if (configItem instanceof ConfigProperty configProperty) {
                        // TODO: we will need to add more metadata on our side
                        SpringConfigMetadataDeprecation deprecation = configProperty.isDeprecated()
                                ? new SpringConfigMetadataDeprecation("warning",
                                        configProperty.getDeprecation().reason(),
                                        configProperty.getDeprecation().replacement(),
                                        configProperty.getDeprecation().since())
                                : null;

                        String description = getJavadoc(javadocRepository, configProperty.getSourceType(),
                                configProperty.getSourceElementName());

                        List<SpringConfigMetadataHintValue> hintValues = List.of();

                        ConfigPhase phase = ConfigPhase.of(configProperty.getPhase());

                        properties.add(new SpringConfigMetadataProperty(configProperty.getPath().property(),
                                configProperty.getType(), description,
                                configProperty.getSourceType(),
                                configProperty.getSourceElementType() == SourceElementType.FIELD
                                        ? configProperty.getSourceElementName()
                                        : null,
                                configProperty.getSourceElementType() == SourceElementType.METHOD
                                        ? configProperty.getSourceElementName()
                                        : null,
                                configProperty.getDefaultValue(),
                                deprecation, new QuarkusConfigAdditionalMetadataProperty(phase,
                                        configProperty.getPath().environmentVariable(), configProperty.isOptional())));
                        if (configProperty.isEnum()) {
                            hintValues = configProperty.getEnumAcceptedValues().values().entrySet().stream()
                                    .map(e -> new SpringConfigMetadataHintValue(e.getValue().configValue(),
                                            getJavadoc(javadocRepository, configProperty.getType(), e.getKey())))
                                    .toList();
                            hints.add(new SpringConfigMetadataHint(configProperty.getPath().property(), hintValues));
                        }

                        for (PropertyPath additionalPath : configProperty.getAdditionalPaths()) {
                            properties.add(
                                    new SpringConfigMetadataProperty(additionalPath.property(), configProperty.getType(),
                                            description, configProperty.getSourceType(),
                                            configProperty.getSourceElementType() == SourceElementType.FIELD
                                                    ? configProperty.getSourceElementName()
                                                    : null,
                                            configProperty.getSourceElementType() == SourceElementType.METHOD
                                                    ? configProperty.getSourceElementName()
                                                    : null,
                                            configProperty.getDefaultValue(),
                                            deprecation, new QuarkusConfigAdditionalMetadataProperty(phase,
                                                    additionalPath.environmentVariable(), configProperty.isOptional())));
                            if (configProperty.isEnum()) {
                                hints.add(new SpringConfigMetadataHint(additionalPath.property(), hintValues));
                            }
                        }
                    }
                }
            });
        }

        return new SpringConfigMetadataModel(groups, properties, hints);
    }

    private List<Path> getMetaInfDirectories() {
        List<Path> classPathElements = new ArrayList<>();
        classPathElements.add(outputDirectory.toPath().resolve(META_INF));

        for (Artifact dependency : project.getArtifacts()) {
            if (!dependency.getArtifactHandler().isAddedToClasspath()) {
                continue;
            }

            Path artifactPath = dependency.getFile().toPath();

            if (Files.isDirectory(artifactPath)) {
                classPathElements.add(dependency.getFile().toPath().resolve(META_INF));
            } else {
                // it's a jar
                try {
                    FileSystem jarFs = FileSystems.newFileSystem(artifactPath);
                    openZipFileSystems.add(jarFs);
                    classPathElements.add(jarFs.getPath("/" + META_INF));
                } catch (IOException e) {
                    getLog().error("Unable to open jar: " + artifactPath, e);
                }
            }
        }

        return Collections.unmodifiableList(classPathElements);
    }

    private Extension getExtension() {
        return new Extension(project.getGroupId(),
                project.getArtifactId().substring(0, project.getArtifactId().length() - DEPLOYMENT_ARTIFACT_SUFFIX.length()),
                null, NameSource.NONE, false, true);
    }

    private String getJavadoc(JavadocRepository javadocRepository, String sourceType, String sourceElementName) {
        Optional<JavadocElement> javadocElement = javadocRepository
                .getElement(sourceType, sourceElementName);
        if (javadocElement.isEmpty()) {
            return null;
        }

        return JavadocTransformer.transform(javadocElement.get().description(),
                javadocElement.get().format(), JavadocFormat.MARKDOWN);
    }
}
