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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;
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
import io.quarkus.annotation.processor.documentation.config.model.AbstractConfigItem;
import io.quarkus.annotation.processor.documentation.config.model.ConfigItemCollection;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.Extension.NameSource;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.model.ResolvedModel;
import io.quarkus.annotation.processor.documentation.config.util.Markers;
import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;

@Mojo(name = "generate-asciidoc", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateAsciidocMojo extends AbstractMojo {

    private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new ParameterNamesModule());
    private static final String TARGET = "target";

    private static final String ADOC_SUFFIX = ".adoc";
    private static final String CONFIG_ROOT_FILE_FORMAT = "%s_%s.adoc";
    private static final String EXTENSION_FILE_FORMAT = "%s.adoc";
    private static final String ALL_CONFIG_FILE_NAME = "quarkus-all-config.adoc";

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Parameter
    private File scanDirectory;

    @Parameter(defaultValue = "${project.build.directory}/quarkus-generated-doc/config", required = true)
    private File targetDirectory;

    @Parameter(defaultValue = "false")
    private boolean generateAllConfig;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }

        // I was unable to find an easy way to get the root directory of the project
        Path resolvedScanDirectory = scanDirectory != null ? scanDirectory.toPath()
                : mavenSession.getCurrentProject().getBasedir().toPath().getParent();
        Path resolvedTargetDirectory = targetDirectory.toPath();
        initTargetDirectory(resolvedTargetDirectory);

        List<Path> targetDirectories = findTargetDirectories(resolvedScanDirectory);

        JavadocRepository javadocRepository = findJavadocElements(targetDirectories);
        MergedModel mergedModel = mergeModel(targetDirectories);

        AsciidocFormatter asciidocFormatter = new AsciidocFormatter(javadocRepository);
        Engine quteEngine = initializeQuteEngine(asciidocFormatter);

        // we generate a file per extension + top level prefix
        for (Entry<Extension, Map<String, ConfigRoot>> extensionConfigRootsEntry : mergedModel.getConfigRoots().entrySet()) {
            Extension extension = extensionConfigRootsEntry.getKey();

            Path configRootAdocPath = null;

            for (Entry<String, ConfigRoot> configRootEntry : extensionConfigRootsEntry.getValue().entrySet()) {
                String topLevelPrefix = configRootEntry.getKey();
                ConfigRoot configRoot = configRootEntry.getValue();

                configRootAdocPath = resolvedTargetDirectory.resolve(String.format(CONFIG_ROOT_FILE_FORMAT,
                        extension.artifactId(), topLevelPrefix));
                String summaryTableId = asciidocFormatter
                        .toAnchor(extension.artifactId() + "_" + topLevelPrefix);

                try {
                    Files.writeString(configRootAdocPath,
                            generateConfigReference(quteEngine, summaryTableId, extension, configRoot, true));
                } catch (Exception e) {
                    throw new MojoExecutionException("Unable to render config roots for top level prefix: " + topLevelPrefix
                            + " in extension: " + extension, e);
                }
            }

            // if we have only one top level prefix, we copy the generated file to a file named after the extension
            // for simplicity's sake
            if (extensionConfigRootsEntry.getValue().size() == 1 && configRootAdocPath != null) {
                Path extensionAdocPath = resolvedTargetDirectory.resolve(String.format(EXTENSION_FILE_FORMAT,
                        extension.artifactId()));

                try {
                    Files.copy(configRootAdocPath, extensionAdocPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new MojoExecutionException("Unable to copy extension file for: " + extension, e);
                }
            }
        }

        // we generate the config roots that are saved in a specific file
        for (Entry<String, ConfigRoot> specificFileConfigRootEntry : mergedModel.getConfigRootsInSpecificFile().entrySet()) {
            String fileName = specificFileConfigRootEntry.getKey();
            ConfigRoot configRoot = specificFileConfigRootEntry.getValue();
            Extension extension = configRoot.getExtension();

            if (!fileName.endsWith(".adoc")) {
                fileName += ".adoc";
            }

            Path configRootAdocPath = resolvedTargetDirectory.resolve(fileName);
            String summaryTableId = asciidocFormatter.toAnchor(stripAdocSuffix(fileName));

            try {
                Files.writeString(configRootAdocPath,
                        generateConfigReference(quteEngine, summaryTableId, extension, configRoot, true));
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to render config roots for specific file: " + fileName
                        + " in extension: " + extension, e);
            }
        }

        // we generate files for generated sections
        for (Entry<Extension, List<ConfigSection>> extensionConfigSectionsEntry : mergedModel.getGeneratedConfigSections()
                .entrySet()) {
            Extension extension = extensionConfigSectionsEntry.getKey();

            for (ConfigSection generatedConfigSection : extensionConfigSectionsEntry.getValue()) {
                Path configSectionAdocPath = resolvedTargetDirectory.resolve(String.format(CONFIG_ROOT_FILE_FORMAT,
                        extension.artifactId(), cleanSectionPath(generatedConfigSection.getPath())));
                String summaryTableId = asciidocFormatter
                        .toAnchor(extension.artifactId() + "_" + generatedConfigSection.getPath());

                try {
                    Files.writeString(configSectionAdocPath,
                            generateConfigReference(quteEngine, summaryTableId, extension, generatedConfigSection, false));
                } catch (Exception e) {
                    throw new MojoExecutionException(
                            "Unable to render config section for section: " + generatedConfigSection.getPath()
                                    + " in extension: " + extension,
                            e);
                }
            }
        }

        if (generateAllConfig) {
            // we generate the file centralizing all the config properties
            try {
                Path allConfigAdocPath = resolvedTargetDirectory.resolve(ALL_CONFIG_FILE_NAME);

                Files.writeString(allConfigAdocPath, generateAllConfig(quteEngine, mergedModel.getConfigRoots()));
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to render all config", e);
            }
        }
    }

    private static String generateConfigReference(Engine quteEngine, String summaryTableId, Extension extension,
            ConfigItemCollection configItemCollection, boolean searchable) {
        return quteEngine.getTemplate("configReference.qute.adoc")
                .data("extension", extension)
                .data("configItemCollection", configItemCollection)
                .data("searchable", searchable)
                .data("summaryTableId", summaryTableId)
                .data("includeDurationNote", configItemCollection.hasDurationType())
                .data("includeMemorySizeNote", configItemCollection.hasMemorySizeType())
                .render();
    }

    private static String generateAllConfig(Engine quteEngine,
            Map<Extension, Map<String, ConfigRoot>> configRootsByExtensions) {
        return quteEngine.getTemplate("allConfig.qute.adoc")
                .data("configRootsByExtensions", configRootsByExtensions)
                .data("searchable", true)
                .data("summaryTableId", "all-config")
                .data("includeDurationNote", true)
                .data("includeMemorySizeNote", true)
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

    private static MergedModel mergeModel(List<Path> targetDirectories) throws MojoExecutionException {
        // keyed on extension and then top level prefix
        Map<Extension, Map<String, ConfigRoot>> configRoots = new HashMap<>();
        // keyed on file name
        Map<String, ConfigRoot> configRootsInSpecificFile = new TreeMap<>();
        // keyed on extension
        Map<Extension, List<ConfigSection>> generatedConfigSections = new TreeMap<>();

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

                for (ConfigRoot configRoot : resolvedModel.getConfigRoots()) {
                    if (configRoot.getOverriddenDocFileName() != null) {
                        ConfigRoot existingConfigRootInSpecificFile = configRootsInSpecificFile
                                .get(configRoot.getOverriddenDocFileName());

                        if (existingConfigRootInSpecificFile == null) {
                            configRootsInSpecificFile.put(configRoot.getOverriddenDocFileName(), configRoot);
                        } else {
                            if (!existingConfigRootInSpecificFile.getExtension().equals(configRoot.getExtension())
                                    || !existingConfigRootInSpecificFile.getPrefix().equals(configRoot.getPrefix())) {
                                throw new MojoExecutionException(
                                        "Two config roots with different extensions or prefixes cannot be merged in the same specific config file: "
                                                + configRoot.getOverriddenDocFileName());
                            }

                            existingConfigRootInSpecificFile.merge(configRoot);
                        }

                        continue;
                    }

                    String topLevelPrefix = getTopLevelPrefix(configRoot.getPrefix());

                    Map<String, ConfigRoot> extensionConfigRoots = configRoots.computeIfAbsent(configRoot.getExtension(),
                            e -> new HashMap<>());

                    ConfigRoot existingConfigRoot = extensionConfigRoots.get(topLevelPrefix);

                    if (existingConfigRoot == null) {
                        extensionConfigRoots.put(topLevelPrefix, configRoot);
                    } else {
                        existingConfigRoot.merge(configRoot);
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to parse: " + javadocPath, e);
            }
        }

        configRoots = retainBestExtensionKey(configRoots);

        for (Entry<Extension, Map<String, ConfigRoot>> extensionConfigRootsEntry : configRoots.entrySet()) {
            List<ConfigSection> extensionGeneratedConfigSections = generatedConfigSections
                    .computeIfAbsent(extensionConfigRootsEntry.getKey(), e -> new ArrayList<>());

            for (ConfigRoot configRoot : extensionConfigRootsEntry.getValue().values()) {
                collectGeneratedConfigSections(extensionGeneratedConfigSections, configRoot);
            }
        }

        return new MergedModel(configRoots, configRootsInSpecificFile, generatedConfigSections);
    }

    private static Map<Extension, Map<String, ConfigRoot>> retainBestExtensionKey(
            Map<Extension, Map<String, ConfigRoot>> configRoots) {
        return configRoots.entrySet().stream().collect(Collectors.toMap(e -> {
            Extension extension = e.getKey();

            for (ConfigRoot configRoot : e.getValue().values()) {
                if (configRoot.getExtension().nameSource().isBetterThan(extension.nameSource())) {
                    extension = configRoot.getExtension();
                }
                if (NameSource.EXTENSION_METADATA.equals(extension.nameSource())) {
                    // we won't find any better
                    break;
                }
            }

            return extension;
        }, e -> e.getValue(), (k1, k2) -> k1, TreeMap::new));
    }

    private static void collectGeneratedConfigSections(List<ConfigSection> extensionGeneratedConfigSections,
            ConfigItemCollection configItemCollection) {
        for (AbstractConfigItem configItem : configItemCollection.getItems()) {
            if (!configItem.isSection()) {
                continue;
            }

            ConfigSection configSection = (ConfigSection) configItem;
            if (configSection.isGenerated()) {
                extensionGeneratedConfigSections.add(configSection);
            }

            collectGeneratedConfigSections(extensionGeneratedConfigSections, configSection);
        }
    }

    private static String getTopLevelPrefix(String prefix) {
        String[] prefixSegments = prefix.split(Pattern.quote(Markers.DOT));

        if (prefixSegments.length == 1) {
            return prefixSegments[0];
        }

        return prefixSegments[0] + Markers.DOT + prefixSegments[1];
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

            // Make sure we are deterministic
            Collections.sort(targets);

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
                        .applyToName("toAnchor")
                        .applyToParameters(1)
                        .resolveAsync(ctx -> ctx.evaluate(ctx.getParams().get(0))
                                .thenApply(o -> asciidocFormatter.toAnchor(
                                        ((Extension) o).artifactId() + "_" + ((ConfigProperty) ctx.getBase()).getPath())))
                        .build())
                // we need a different anchor for sections as otherwise we can have a conflict
                // (typically when you have an `enabled` property with parent name just under the section level)
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigSection.class)
                        .applyToName("toAnchor")
                        .applyToParameters(1)
                        .resolveAsync(ctx -> ctx.evaluate(ctx.getParams().get(0))
                                .thenApply(o -> asciidocFormatter.toAnchor(
                                        ((Extension) o).artifactId() + "_section_"
                                                + ((ConfigSection) ctx.getBase()).getPath())))
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
        engine.putTemplate("allConfig.qute.adoc",
                engine.parse(getTemplate("templates/allConfig.qute.adoc")));
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

    /**
     * A section path can contain quotes when being inside a Map.
     * <p>
     * While not very common, we sometimes have to generate a section inside a Map
     * e.g. for the XDS config of the gRPC client.
     */
    private static String cleanSectionPath(String sectionPath) {
        return sectionPath.replace('"', '-');
    }

    private String stripAdocSuffix(String fileName) {
        return fileName.substring(0, fileName.length() - ADOC_SUFFIX.length());
    }
}
