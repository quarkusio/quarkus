package io.quarkus.maven.config.doc.generator;

import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.merger.MergedModel.ConfigRootKey;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.maven.config.doc.GenerateConfigDocMojo.Context;

public interface Formatter {

    boolean displayConfigRootDescription(ConfigRootKey configRootKey, int mapSize);

    String formatDescription(ConfigProperty configProperty);

    default String formatDescription(ConfigProperty configProperty, Extension extension, Context context) {
        return formatDescription(configProperty);
    }

    String formatTypeDescription(ConfigProperty configProperty, Context context);

    String formatDefaultValue(ConfigProperty configProperty);

    int adjustedLevel(ConfigSection configSection, boolean multiRoot);

    String escapeCellContent(String value);

    String toAnchor(String value);

    String formatSectionTitle(ConfigSection configSection);

    String formatName(Extension extension);

    static Formatter getFormatter(GenerationReport generationReport, JavadocRepository javadocRepository,
            boolean enableEnumTooltips, Format format) {
        switch (format) {
            case asciidoc:
                return new AsciidocFormatter(generationReport, javadocRepository, enableEnumTooltips);
            case markdown:
                return new MarkdownFormatter(generationReport, javadocRepository);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
}