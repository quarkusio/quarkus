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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.annotation.processor.documentation.config.merger.JavadocMerger;
import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.merger.MergedModel;
import io.quarkus.annotation.processor.documentation.config.merger.MergedModel.ConfigRootKey;
import io.quarkus.annotation.processor.documentation.config.merger.ModelMerger;
import io.quarkus.annotation.processor.documentation.config.model.ConfigItemCollection;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.maven.config.doc.generator.Format;
import io.quarkus.maven.config.doc.generator.Formatter;
import io.quarkus.maven.config.doc.generator.GenerationReport;
import io.quarkus.maven.config.doc.generator.GenerationReport.GenerationViolation;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;

@Mojo(name = "generate-config-doc", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateConfigDocMojo extends AbstractMojo {

    private static final String TARGET = "target";

    private static final String ADOC_SUFFIX = ".adoc";
    private static final String CONFIG_ROOT_FILE_FORMAT = "%s_%s.%s";
    private static final String EXTENSION_FILE_FORMAT = "%s.%s";
    private static final String ALL_CONFIG_FILE_FORMAT = "quarkus-all-config.%s";

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    // if this is switched to something else at some point, GenerateAsciidocMojo will need to be adapted
    @Parameter(defaultValue = "asciidoc")
    private String format;

    @Parameter(defaultValue = Format.DEFAULT_THEME)
    private String theme;

    @Parameter
    private File scanDirectory;

    @Parameter(defaultValue = "${project.build.directory}/quarkus-generated-doc/config", required = true)
    private File targetDirectory;

    @Parameter(defaultValue = "false")
    private boolean generateAllConfig;

    @Parameter(defaultValue = "false")
    private boolean enableEnumTooltips;

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

        GenerationReport generationReport = new GenerationReport();
        JavadocRepository javadocRepository = JavadocMerger.mergeJavadocElements(targetDirectories);
        MergedModel mergedModel = ModelMerger.mergeModel(javadocRepository, targetDirectories, true);

        Format normalizedFormat = Format.normalizeFormat(format);

        String normalizedTheme = normalizedFormat.normalizeTheme(theme);
        Formatter formatter = Formatter.getFormatter(generationReport, javadocRepository, enableEnumTooltips, normalizedFormat);
        Engine quteEngine = initializeQuteEngine(formatter, normalizedFormat, normalizedTheme);

        // we generate a file per extension + top level prefix
        for (Entry<Extension, Map<ConfigRootKey, ConfigRoot>> extensionConfigRootsEntry : mergedModel.getConfigRoots()
                .entrySet()) {
            Extension extension = extensionConfigRootsEntry.getKey();

            Path configRootPath = null;

            for (Entry<ConfigRootKey, ConfigRoot> configRootEntry : extensionConfigRootsEntry.getValue().entrySet()) {
                String topLevelPrefix = configRootEntry.getKey().topLevelPrefix();
                ConfigRoot configRoot = configRootEntry.getValue();

                // here we generate a file even if there are no items as it's used for the Reactive Oracle SQL client

                configRootPath = resolvedTargetDirectory.resolve(String.format(CONFIG_ROOT_FILE_FORMAT,
                        extension.artifactId(), topLevelPrefix, normalizedFormat.getExtension()));
                String summaryTableId = formatter
                        .toAnchor(extension.artifactId() + "_" + topLevelPrefix);
                Context context = new Context(summaryTableId, false);

                try {
                    Files.writeString(configRootPath,
                            generateConfigReference(quteEngine, context, extension, configRoot, "", true));
                } catch (Exception e) {
                    throw new MojoExecutionException("Unable to render config roots for top level prefix: " + topLevelPrefix
                            + " in extension: " + extension, e);
                }
            }

            // if we have only one top level prefix, we copy the generated file to a file named after the extension
            // for simplicity's sake
            if (extensionConfigRootsEntry.getValue().size() == 1 && configRootPath != null) {
                Path extensionPath = resolvedTargetDirectory.resolve(String.format(EXTENSION_FILE_FORMAT,
                        extension.artifactId(), normalizedFormat.getExtension()));

                try {
                    Files.copy(configRootPath, extensionPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new MojoExecutionException("Unable to copy extension file for: " + extension, e);
                }
            }
        }

        // we generate the config roots that are saved in a specific file
        for (Entry<String, ConfigRoot> specificFileConfigRootEntry : mergedModel.getConfigRootsInSpecificFile().entrySet()) {
            String annotationFileName = specificFileConfigRootEntry.getKey();
            ConfigRoot configRoot = specificFileConfigRootEntry.getValue();
            Extension extension = configRoot.getExtension();

            if (configRoot.getNonDeprecatedItems().isEmpty()) {
                continue;
            }

            String normalizedFileName = stripAdocSuffix(annotationFileName);
            String fileName = normalizedFileName + "." + normalizedFormat.getExtension();

            Path configRootPath = resolvedTargetDirectory.resolve(fileName);
            String summaryTableId = formatter.toAnchor(normalizedFileName);
            Context context = new Context(summaryTableId, false);

            try {
                Files.writeString(configRootPath,
                        generateConfigReference(quteEngine, context, extension, configRoot, "", true));
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to render config roots for specific file: " + fileName
                        + " in extension: " + extension, e);
            }
        }

        if (!generationReport.getViolations().isEmpty()) {
            StringBuilder report = new StringBuilder(
                    "One or more errors happened during the configuration documentation generation. Here is a full report:\n\n");
            for (Entry<String, List<GenerationViolation>> violationsEntry : generationReport.getViolations().entrySet()) {
                report.append("- ").append(violationsEntry.getKey()).append("\n");
                for (GenerationViolation violation : violationsEntry.getValue()) {
                    report.append("    . ").append(violation.sourceElement()).append(" - ").append(violation.message())
                            .append("\n");
                }
                report.append("\n----\n\n");
            }

            throw new IllegalStateException(report.toString());
        }

        // we generate files for generated sections
        for (Entry<Extension, List<ConfigSection>> extensionConfigSectionsEntry : mergedModel.getGeneratedConfigSections()
                .entrySet()) {
            Extension extension = extensionConfigSectionsEntry.getKey();

            for (ConfigSection generatedConfigSection : extensionConfigSectionsEntry.getValue()) {
                if (generatedConfigSection.getNonDeprecatedItems().isEmpty()) {
                    continue;
                }

                Path configSectionPath = resolvedTargetDirectory.resolve(String.format(CONFIG_ROOT_FILE_FORMAT,
                        extension.artifactId(), cleanSectionPath(generatedConfigSection.getPath().property()),
                        normalizedFormat.getExtension()));
                String summaryTableId = formatter
                        .toAnchor(extension.artifactId() + "_" + generatedConfigSection.getPath().property());
                Context context = new Context(summaryTableId, false);

                try {
                    Files.writeString(configSectionPath,
                            generateConfigReference(quteEngine, context, extension, generatedConfigSection,
                                    "_" + generatedConfigSection.getPath().property(), false));
                } catch (Exception e) {
                    throw new MojoExecutionException(
                            "Unable to render config section for section: " + generatedConfigSection.getPath().property()
                                    + " in extension: " + extension,
                            e);
                }
            }
        }

        if (generateAllConfig) {
            // we generate the file centralizing all the config properties
            try {
                Path allConfigPath = resolvedTargetDirectory.resolve(String.format(ALL_CONFIG_FILE_FORMAT,
                        normalizedFormat.getExtension()));

                Context context = new Context("all-config", true);

                Files.writeString(allConfigPath, generateAllConfig(quteEngine, context, mergedModel.getConfigRoots()));
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to render all config", e);
            }
        }
    }

    private static String generateConfigReference(Engine quteEngine, Context context, Extension extension,
            ConfigItemCollection configItemCollection, String additionalAnchorPrefix, boolean searchable) {
        return quteEngine.getTemplate("configReference")
                .data("extension", extension)
                .data("configItemCollection", configItemCollection)
                .data("searchable", searchable)
                .data("context", context)
                .data("summaryTableId", context.summaryTableId()) // for backward compatibility, use context instead
                .data("additionalAnchorPrefix", additionalAnchorPrefix)
                .data("includeDurationNote", configItemCollection.hasDurationType())
                .data("includeMemorySizeNote", configItemCollection.hasMemorySizeType())
                .render();
    }

    private static String generateAllConfig(Engine quteEngine, Context context,
            Map<Extension, Map<ConfigRootKey, ConfigRoot>> configRootsByExtensions) {
        return quteEngine.getTemplate("allConfig")
                .data("configRootsByExtensions", configRootsByExtensions)
                .data("searchable", true)
                .data("context", context)
                .data("summaryTableId", context.summaryTableId()) // for backward compatibility, use context instead
                .data("additionalAnchorPrefix", "")
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

    private static List<Path> findTargetDirectories(Path scanDirectory) throws MojoExecutionException {
        try {
            List<Path> targets = new ArrayList<>();

            Files.walkFileTree(scanDirectory, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.endsWith(TARGET)) {
                        // we check if there is a POM around as it might happen that the target/ directory is still around
                        // while the module has been dropped
                        if (Files.exists(dir.resolve("../pom.xml"))) {
                            targets.add(dir);
                        }

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

    private static Engine initializeQuteEngine(Formatter formatter, Format format, String theme) {
        EngineBuilder engineBuilder = Engine.builder()
                .addDefaults()
                .addSectionHelper(new UserTagSectionHelper.Factory("configProperty", "configProperty"))
                .addSectionHelper(new UserTagSectionHelper.Factory("configSection", "configSection"))
                .addSectionHelper(new UserTagSectionHelper.Factory("envVar", "envVar"))
                .addSectionHelper(new UserTagSectionHelper.Factory("durationNote", "durationNote"))
                .addSectionHelper(new UserTagSectionHelper.Factory("memorySizeNote", "memorySizeNote"))
                .addValueResolver(new ReflectionValueResolver())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(String.class)
                        .applyToName("escapeCellContent")
                        .applyToNoParameters()
                        .resolveSync(ctx -> formatter.escapeCellContent((String) ctx.getBase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(String.class)
                        .applyToName("toAnchor")
                        .applyToNoParameters()
                        .resolveSync(ctx -> formatter.toAnchor((String) ctx.getBase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigRootKey.class)
                        .applyToName("displayConfigRootDescription")
                        .applyToParameters(1)
                        .resolveSync(ctx -> formatter
                                .displayConfigRootDescription((ConfigRootKey) ctx.getBase(),
                                        (int) ctx.evaluate(ctx.getParams().get(0)).toCompletableFuture().join()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigProperty.class)
                        .applyToName("toAnchor")
                        .applyToParameters(2)
                        .resolveSync(ctx -> formatter
                                .toAnchor(((Extension) ctx.evaluate(ctx.getParams().get(0)).toCompletableFuture().join())
                                        .artifactId() +
                                // the additional suffix
                                        ctx.evaluate(ctx.getParams().get(1)).toCompletableFuture().join() +
                                        "_" + ((ConfigProperty) ctx.getBase()).getPath().property()))
                        .build())
                // we need a different anchor for sections as otherwise we can have a conflict
                // (typically when you have an `enabled` property with parent name just under the section level)
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigSection.class)
                        .applyToName("toAnchor")
                        .applyToParameters(2)
                        .resolveSync(ctx -> formatter
                                .toAnchor(((Extension) ctx.evaluate(ctx.getParams().get(0)).toCompletableFuture().join())
                                        .artifactId() +
                                // the additional suffix
                                        ctx.evaluate(ctx.getParams().get(1)).toCompletableFuture().join() +
                                        "_section_" + ((ConfigSection) ctx.getBase()).getPath().property()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigProperty.class)
                        .applyToName("formatTypeDescription")
                        .applyToParameters(1)
                        .resolveSync(ctx -> formatter.formatTypeDescription((ConfigProperty) ctx.getBase(),
                                (Context) ctx.evaluate(ctx.getParams().get(0)).toCompletableFuture().join()))
                        .build())
                // deprecated, for backward compatibility
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigProperty.class)
                        .applyToName("formatTypeDescription")
                        .applyToNoParameters()
                        .resolveSync(ctx -> formatter.formatTypeDescription((ConfigProperty) ctx.getBase(), null))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigProperty.class)
                        .applyToName("formatDescription")
                        .applyToNoParameters()
                        .resolveSync(ctx -> formatter.formatDescription((ConfigProperty) ctx.getBase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigProperty.class)
                        .applyToName("formatDescription")
                        .applyToParameters(2)
                        .resolveSync(ctx -> formatter
                                .formatDescription((ConfigProperty) ctx.getBase(),
                                        (Extension) ctx.evaluate(ctx.getParams().get(0)).toCompletableFuture().join(),
                                        (Context) ctx.evaluate(ctx.getParams().get(1)).toCompletableFuture().join()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigProperty.class)
                        .applyToName("formatDefaultValue")
                        .applyToNoParameters()
                        .resolveSync(ctx -> formatter.formatDefaultValue((ConfigProperty) ctx.getBase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigSection.class)
                        .applyToName("formatTitle")
                        .applyToNoParameters()
                        .resolveSync(ctx -> formatter.formatSectionTitle((ConfigSection) ctx.getBase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(ConfigSection.class)
                        .applyToName("adjustedLevel")
                        .applyToParameters(1)
                        .resolveSync(ctx -> formatter
                                .adjustedLevel((ConfigSection) ctx.getBase(),
                                        (boolean) ctx.evaluate(ctx.getParams().get(0)).toCompletableFuture().join()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(Extension.class)
                        .applyToName("formatName")
                        .applyToNoParameters()
                        .resolveSync(ctx -> formatter.formatName((Extension) ctx.getBase()))
                        .build());

        if (format == Format.asciidoc) {
            engineBuilder.addSectionHelper(new UserTagSectionHelper.Factory("propertyCopyButton", "propertyCopyButton"));
        }

        Engine engine = engineBuilder.build();
        engine.putTemplate("configReference",
                engine.parse(getTemplate("templates", format, theme, "configReference", false)));
        engine.putTemplate("allConfig",
                engine.parse(getTemplate("templates", format, theme, "allConfig", false)));
        engine.putTemplate("configProperty",
                engine.parse(getTemplate("templates", format, theme, "configProperty", true)));
        engine.putTemplate("configSection",
                engine.parse(getTemplate("templates", format, theme, "configSection", true)));
        engine.putTemplate("envVar",
                engine.parse(getTemplate("templates", format, theme, "envVar", true)));
        engine.putTemplate("durationNote",
                engine.parse(getTemplate("templates", format, theme, "durationNote", true)));
        engine.putTemplate("memorySizeNote",
                engine.parse(getTemplate("templates", format, theme, "memorySizeNote", true)));

        if (format == Format.asciidoc) {
            engine.putTemplate("propertyCopyButton",
                    engine.parse(getTemplate("templates", format, theme, "propertyCopyButton", true)));
        }

        return engine;
    }

    private static String getTemplate(String root, Format format, String theme, String template, boolean tag) {
        List<String> candidates = new ArrayList<>();
        candidates.add(
                root + "/" + format + "/" + theme + "/" + (tag ? "tags/" : "") + template + ".qute." + format.getExtension());
        if (!Format.DEFAULT_THEME.equals(theme)) {
            candidates
                    .add(root + "/" + format + "/" + Format.DEFAULT_THEME + "/" + (tag ? "tags/" : "") + template + ".qute."
                            + format.getExtension());
        }

        InputStream is = null;
        ;
        for (String candidate : candidates) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(candidate);
            if (is != null) {
                break;
            }
        }

        if (is == null) {
            throw new IllegalArgumentException("Unable to find a template for these candidates " + candidates);
        }

        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the template: " + template, e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to close InputStream for template: " + template, e);
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

    public record Context(String summaryTableId, boolean allConfig) {
    }
}
