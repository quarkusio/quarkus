package io.quarkus.maven.config.doc;

import java.text.Normalizer;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.util.Types;

final class AsciidocFormatter {

    private static final String TOOLTIP_MACRO = "tooltip:%s[%s]";
    private static final String MORE_INFO_ABOUT_TYPE_FORMAT = "link:#%s[icon:question-circle[title=More information about the %s format]]";

    private final JavadocRepository javadocRepository;
    private final boolean enableEnumTooltips;

    AsciidocFormatter(JavadocRepository javadocRepository, boolean enableEnumTooltips) {
        this.javadocRepository = javadocRepository;
        this.enableEnumTooltips = enableEnumTooltips;
    }

    String formatDescription(ConfigProperty configProperty) {
        Optional<JavadocElement> javadocElement = javadocRepository.getElement(configProperty.getSourceClass(),
                configProperty.getSourceName());

        if (javadocElement.isEmpty()) {
            return null;
        }

        String description = javadocElement.get().description();
        if (description == null || description.isBlank()) {
            return null;
        }

        return description + "\n\n";
    }

    String formatTypeDescription(ConfigProperty configProperty) {
        String typeContent = "";

        if (configProperty.isEnum()) {
            if (enableEnumTooltips) {
                typeContent = configProperty.getEnumAcceptedValues().values().entrySet().stream()
                        .map(e -> {
                            Optional<JavadocElement> javadocElement = javadocRepository.getElement(configProperty.getType(),
                                    e.getKey());
                            if (javadocElement.isEmpty()) {
                                return "`" + e.getValue().configValue() + "`";
                            }

                            return String.format(TOOLTIP_MACRO, e.getValue().configValue(),
                                    cleanTooltipContent(javadocElement.get().description()));
                        })
                        .collect(Collectors.joining(", "));
            } else {
                typeContent = configProperty.getEnumAcceptedValues().values().values().stream()
                        .map(v -> v.configValue())
                        .collect(Collectors.joining("`, `", "`", "`"));
            }
        } else {
            typeContent = configProperty.getTypeDescription();
            if (configProperty.getJavadocSiteLink() != null) {
                typeContent = String.format("link:%s[%s]", configProperty.getJavadocSiteLink(), typeContent);
            }
        }
        if (configProperty.isList()) {
            typeContent = "list of " + typeContent;
        }

        if (Duration.class.getName().equals(configProperty.getType())) {
            typeContent += " " + String.format(MORE_INFO_ABOUT_TYPE_FORMAT,
                    "duration-note-anchor-{summaryTableId}", Duration.class.getSimpleName());
        } else if (Types.MEMORY_SIZE_TYPE.equals(configProperty.getType())) {
            typeContent += " " + String.format(MORE_INFO_ABOUT_TYPE_FORMAT,
                    "memory-size-note-anchor-{summaryTableId}", "MemorySize");
        }

        return typeContent;
    }

    String formatDefaultValue(ConfigProperty configProperty) {
        String defaultValue = configProperty.getDefaultValue();

        if (defaultValue == null) {
            return null;
        }

        if (configProperty.isEnum() && enableEnumTooltips) {
            Optional<String> enumConstant = configProperty.getEnumAcceptedValues().values().entrySet().stream()
                    .filter(e -> e.getValue().configValue().equals(defaultValue))
                    .map(e -> e.getKey())
                    .findFirst();

            if (enumConstant.isPresent()) {
                Optional<JavadocElement> javadocElement = javadocRepository.getElement(configProperty.getType(),
                        enumConstant.get());

                if (javadocElement.isPresent()) {
                    return String.format(TOOLTIP_MACRO, defaultValue, cleanTooltipContent(javadocElement.get().description()));
                }
            }
        }

        return "`" + defaultValue + "`";
    }

    String escapeCellContent(String value) {
        if (value == null) {
            return null;
        }

        return value.replace("|", "\\|");
    }

    String toAnchor(String value) {
        // remove accents
        value = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("[àáâãäåāąă]", "a")
                .replaceAll("[çćčĉċ]", "c")
                .replaceAll("[ďđð]", "d")
                .replaceAll("[èéêëēęěĕė]", "e")
                .replaceAll("[ƒſ]", "f")
                .replaceAll("[ĝğġģ]", "g")
                .replaceAll("[ĥħ]", "h")
                .replaceAll("[ìíîïīĩĭįı]", "i")
                .replaceAll("[ĳĵ]", "j")
                .replaceAll("[ķĸ]", "k")
                .replaceAll("[łľĺļŀ]", "l")
                .replaceAll("[ñńňņŉŋ]", "n")
                .replaceAll("[òóôõöøōőŏœ]", "o")
                .replaceAll("[Þþ]", "p")
                .replaceAll("[ŕřŗ]", "r")
                .replaceAll("[śšşŝș]", "s")
                .replaceAll("[ťţŧț]", "t")
                .replaceAll("[ùúûüūůűŭũų]", "u")
                .replaceAll("[ŵ]", "w")
                .replaceAll("[ýÿŷ]", "y")
                .replaceAll("[žżź]", "z")
                .replaceAll("[æ]", "ae")
                .replaceAll("[ÀÁÂÃÄÅĀĄĂ]", "A")
                .replaceAll("[ÇĆČĈĊ]", "C")
                .replaceAll("[ĎĐÐ]", "D")
                .replaceAll("[ÈÉÊËĒĘĚĔĖ]", "E")
                .replaceAll("[ĜĞĠĢ]", "G")
                .replaceAll("[ĤĦ]", "H")
                .replaceAll("[ÌÍÎÏĪĨĬĮİ]", "I")
                .replaceAll("[Ĵ]", "J")
                .replaceAll("[Ķ]", "K")
                .replaceAll("[ŁĽĹĻĿ]", "L")
                .replaceAll("[ÑŃŇŅŊ]", "N")
                .replaceAll("[ÒÓÔÕÖØŌŐŎ]", "O")
                .replaceAll("[ŔŘŖ]", "R")
                .replaceAll("[ŚŠŞŜȘ]", "S")
                .replaceAll("[ÙÚÛÜŪŮŰŬŨŲ]", "U")
                .replaceAll("[Ŵ]", "W")
                .replaceAll("[ÝŶŸ]", "Y")
                .replaceAll("[ŹŽŻ]", "Z")
                .replaceAll("[ß]", "ss");

        // TODO cache the patterns in statics
        // Apostrophes.
        value = value.replaceAll("([a-z])'s([^a-z])", "$1s$2");
        // Allow only letters, -, _
        value = value.replaceAll("[^\\w-_]", "-").replaceAll("-{2,}", "-");
        // Get rid of any - at the start and end.
        value = value.replaceAll("-+$", "").replaceAll("^-+", "");

        return value.toLowerCase();
    }

    String formatSectionTitle(ConfigSection configSection) {
        Optional<JavadocElement> javadocElement = javadocRepository.getElement(configSection.getSourceClass(),
                configSection.getSourceName());

        if (javadocElement.isEmpty()) {
            throw new IllegalStateException(
                    "Couldn't find section title for: " + configSection.getSourceClass() + "#" + configSection.getSourceName());
        }

        String javadoc = javadocElement.get().description();
        if (javadoc == null || javadoc.isBlank()) {
            throw new IllegalStateException(
                    "Couldn't find section title for: " + configSection.getSourceClass() + "#" + configSection.getSourceName());
        }

        javadoc = javadoc.trim();
        int dotIndex = javadoc.indexOf(".");

        if (dotIndex == -1) {
            return javadoc;
        }

        return javadoc.substring(0, dotIndex);
    }

    String formatName(Extension extension) {
        if (extension.name() == null) {
            return extension.artifactId();
        }

        return extension.name();
    }

    /**
     * Note that this is extremely brittle. Apparently, colons breaks the tooltips but if escaped with \, the \ appears in the
     * output.
     * <p>
     * We should probably have some warnings/errors as to what is accepted in enum Javadoc.
     */
    private String cleanTooltipContent(String tooltipContent) {
        return tooltipContent.replace("<p>", "").replace("</p>", "").replace("\n+\n", " ").replace("\n", " ")
                .replace(":", "\\:").replace("[", "\\]").replace("]", "\\]");
    }
}
