package io.quarkus.maven.config.doc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.quarkus.annotation.processor.Outputs;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.model.ResolvedModel;
import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;

@Mojo(name = "generate-asciidoc", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateAsciidocMojo extends AbstractMojo {

    private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new ParameterNamesModule());
    private static final String TARGET = "target";

    private static final String CONFIG_ROOT_FILE_FORMAT = "%s_%s.adoc";
    private static final String EXTENSION_FILE_FORMAT = "%s.adoc";

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Parameter(readonly = true)
    private File scanDirectory;

    @Parameter(defaultValue = "${project.build.directory}/quarkus-config-doc", readonly = true, required = true)
    private File targetDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // I was unable to find an easy way to get the root directory of the project
        Path resolvedScanDirectory = scanDirectory != null ? scanDirectory.toPath()
                : mavenSession.getCurrentProject().getBasedir().toPath().getParent();
        Path resolvedTargetDirectory = targetDirectory.toPath();
        initTargetDirectory(resolvedTargetDirectory);

        List<Path> targetDirectories = findTargetDirectories(resolvedScanDirectory);

        JavadocRepository javadocRepository = findJavadocElements(targetDirectories);
        Map<ConfigRootKey, ConfigRoot> configRoots = findConfigRoots(targetDirectories);

        AsciidocFormatter asciidocFormatter = new AsciidocFormatter(javadocRepository);
        Engine quteEngine = initializeQuteEngine(asciidocFormatter);

        // we generate a file per extension + top level prefix
        for (Entry<ConfigRootKey, ConfigRoot> configRootEntry : configRoots.entrySet()) {
            ConfigRootKey configRootKey = configRootEntry.getKey();
            ConfigRoot configRoot = configRootEntry.getValue();

            Path configRootAdocPath = resolvedTargetDirectory.resolve(String.format(CONFIG_ROOT_FILE_FORMAT,
                    configRootKey.getArtifactId(), configRootKey.getTopLevelPrefix()));
            String summaryTableId = asciidocFormatter
                    .toAnchor(configRootKey.getArtifactId() + "_" + configRootKey.getTopLevelPrefix());

            try {
                Files.writeString(configRootAdocPath,
                        generateConfigReference(quteEngine, summaryTableId, configRootKey, configRoot));
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to render config root: " + configRootKey, e);
            }
        }

        // for extensions with only one top level prefix, we also copy the file to an extension file
        Map<String, List<ConfigRootKey>> configRootKeysPerExtension = configRoots.keySet().stream()
                .collect(Collectors.groupingBy(crk -> crk.getGroupId() + ":" + crk.getArtifactId()));

        for (List<ConfigRootKey> extensionConfigRootKeys : configRootKeysPerExtension.values()) {
            if (extensionConfigRootKeys.size() != 1) {
                continue;
            }

            ConfigRootKey configRootKey = extensionConfigRootKeys.get(0);

            Path extensionAdocPath = resolvedTargetDirectory.resolve(String.format(EXTENSION_FILE_FORMAT,
                    configRootKey.getArtifactId()));
            Path configRootAdocPath = resolvedTargetDirectory.resolve(String.format(CONFIG_ROOT_FILE_FORMAT,
                    configRootKey.getArtifactId(), configRootKey.getTopLevelPrefix()));

            try {
                Files.copy(configRootAdocPath, extensionAdocPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to copy extension file for: " + configRootKey, e);
            }
        }
    }

    private static String generateConfigReference(Engine quteEngine, String summaryTableId, ConfigRootKey configRootKey,
            ConfigRoot configRoot) {
        return quteEngine.getTemplate("configReference.qute.adoc")
                .data("configRootKey", configRootKey)
                .data("configRoot", configRoot)
                .data("searchable", true)
                .data("summaryTableId", summaryTableId)
                .data("includeDurationNote", configRoot.hasDurationType())
                .data("includeMemorySizeNote", configRoot.hasMemorySizeType())
                .render();
    }

    private static void initTargetDirectory(Path resolvedTargetDirectory) throws MojoExecutionException {
        try {
            Files.createDirectories(resolvedTargetDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create directory: " + resolvedTargetDirectory, e);
        }
    }

    private static JavadocRepository findJavadocElements(List<Path> targetDirectories) throws MojoExecutionException {
        Map<String, JavadocElement> javadocElementsMap = new HashMap<>();

        for (Path targetDirectory : targetDirectories) {
            Path javadocPath = targetDirectory.resolve(Outputs.QUARKUS_CONFIG_DOC_JAVADOC);
            if (!Files.isReadable(javadocPath)) {
                continue;
            }

            try {
                JavadocElements javadocElements = YAML_OBJECT_MAPPER.readValue(javadocPath.toFile(), JavadocElements.class);

                if (javadocElements.elements() == null || javadocElements.elements().isEmpty()) {
                    continue;
                }

                javadocElementsMap.putAll(javadocElements.elements());
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to parse: " + javadocPath, e);
            }
        }

        return new JavadocRepository(javadocElementsMap);
    }

    private static Map<ConfigRootKey, ConfigRoot> findConfigRoots(List<Path> targetDirectories) throws MojoExecutionException {
        Map<ConfigRootKey, ConfigRoot> configRootsMap = new HashMap<>();

        for (Path targetDirectory : targetDirectories) {
            Path javadocPath = targetDirectory.resolve(Outputs.QUARKUS_CONFIG_DOC_MODEL);
            if (!Files.isReadable(javadocPath)) {
                continue;
            }

            try {
                ResolvedModel resolvedModel = YAML_OBJECT_MAPPER.readValue(javadocPath.toFile(), ResolvedModel.class);

                if (resolvedModel.getConfigRoots() == null || resolvedModel.getConfigRoots().isEmpty()) {
                    continue;
                }

                for (ConfigRoot configRoot : resolvedModel.getConfigRoots().values()) {
                    ConfigRootKey configRootKey = new ConfigRootKey(configRoot.getExtension().groupId(),
                            configRoot.getExtension().artifactId(),
                            configRoot.getPrefix());

                    ConfigRoot existingConfigRoot = configRootsMap.get(configRootKey);
                    if (existingConfigRoot == null) {
                        configRootsMap.put(configRootKey, configRoot);
                    } else {
                        existingConfigRoot.merge(configRoot);
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to parse: " + javadocPath, e);
            }
        }

        return configRootsMap;
    }

    private static List<Path> findTargetDirectories(Path scanDirectory) throws MojoExecutionException {
        try {
            List<Path> targets = new ArrayList<>();

            Files.walkFileTree(scanDirectory, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.endsWith(TARGET)) {
                        targets.add(dir);

                        // a target directory can contain target directories for test projects
                        // so let's make sure we ignore whatever is nested in a target
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            return targets;
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to collect the target directories", e);
        }
    }

    private static Engine initializeQuteEngine(AsciidocFormatter asciidocFormatter) {
        Engine engine = Engine.builder()
                .addDefaults()
                .addSectionHelper(new UserTagSectionHelper.Factory("configProperty", "configProperty.qute.adoc"))
                .addSectionHelper(new UserTagSectionHelper.Factory("configSection", "configSection.qute.adoc"))
                .addSectionHelper(new UserTagSectionHelper.Factory("envVar", "envVar.qute.adoc"))
                .addSectionHelper(new UserTagSectionHelper.Factory("durationNote", "durationNote.qute.adoc"))
                .addSectionHelper(new UserTagSectionHelper.Factory("memorySizeNote", "memorySizeNote.qute.adoc"))
                .addValueResolver(new ReflectionValueResolver())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(String.class)
                        .applyToName("escapeCellContent")
                        .applyToNoParameters()
                        .resolveSync(ctx -> asciidocFormatter.escapeCellContent((String) ctx.getBase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(String.class)
                        .applyToName("toAnchor")
                        .applyToNoParameters()
                        .resolveSync(ctx -> asciidocFormatter.toAnchor((String) ctx.getBase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigProperty.class)
                        .applyToName("typeDescription")
                        .applyToNoParameters()
                        .resolveSync(ctx -> asciidocFormatter.formatTypeDescription((ConfigProperty) ctx.getBase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigProperty.class)
                        .applyToName("description")
                        .applyToNoParameters()
                        .resolveSync(ctx -> asciidocFormatter.formatDescription((ConfigProperty) ctx.getBase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigSection.class)
                        .applyToName("title")
                        .applyToNoParameters()
                        .resolveSync(ctx -> asciidocFormatter.formatSectionTitle((ConfigSection) ctx.getBase()))
                        .build())
                .build();

        engine.putTemplate("configReference.qute.adoc",
                engine.parse(getTemplate("templates/configReference.qute.adoc")));
        engine.putTemplate("configProperty.qute.adoc",
                engine.parse(getTemplate("templates/tags/configProperty.qute.adoc")));
        engine.putTemplate("configSection.qute.adoc",
                engine.parse(getTemplate("templates/tags/configSection.qute.adoc")));
        engine.putTemplate("envVar.qute.adoc",
                engine.parse(getTemplate("templates/tags/envVar.qute.adoc")));
        engine.putTemplate("durationNote.qute.adoc",
                engine.parse(getTemplate("templates/tags/durationNote.qute.adoc")));
        engine.putTemplate("memorySizeNote.qute.adoc",
                engine.parse(getTemplate("templates/tags/memorySizeNote.qute.adoc")));

        return engine;
    }

    private static String getTemplate(String template) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(template);
        if (is == null) {
            throw new IllegalArgumentException("Template does not exist: " + template);
        }

        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the template: " + template, e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new UncheckedIOException("Unable close InputStream for template: " + template, e);
            }
        }
    }
}
