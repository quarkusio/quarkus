package io.quarkus.maven.config.doc.generator;

import java.text.Normalizer;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.annotation.processor.documentation.config.formatter.JavadocTransformer;
import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.merger.MergedModel.ConfigRootKey;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.maven.config.doc.GenerateConfigDocMojo.Context;

abstract class AbstractFormatter implements Formatter {

    protected final JavadocRepository javadocRepository;
    protected final boolean enableEnumTooltips;

    AbstractFormatter(JavadocRepository javadocRepository, boolean enableEnumTooltips) {
        this.javadocRepository = javadocRepository;
        this.enableEnumTooltips = enableEnumTooltips;
    }

    @Override
    public boolean displayConfigRootDescription(ConfigRootKey configRootKey, int mapSize) {
        if (mapSize <= 1) {
            return false;
        }

        return configRootKey.description() != null;
    }

    @Override
    public String formatDescription(ConfigProperty configProperty) {
        Optional<JavadocElement> javadocElement = javadocRepository.getElement(configProperty.getSourceClass(),
                configProperty.getSourceName());

        if (javadocElement.isEmpty()) {
            return null;
        }

        String description = JavadocTransformer.transform(javadocElement.get().description(), javadocElement.get().format(),
                javadocFormat());
        if (description == null || description.isBlank()) {
            return null;
        }

        return description + "\n\n";
    }

    @Override
    public String formatTypeDescription(ConfigProperty configProperty, Context context) {
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

                            return tooltip(e.getValue().configValue(), JavadocTransformer
                                    .transform(javadocElement.get().description(), javadocElement.get().format(),
                                            javadocFormat()));
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
                typeContent = link(configProperty.getJavadocSiteLink(), typeContent);
            }
        }
        if (configProperty.isList()) {
            typeContent = "list of " + typeContent;
        }

        if (Duration.class.getName().equals(configProperty.getType())) {
            typeContent += " " + moreInformationAboutType(context, "duration-note-anchor", Duration.class.getSimpleName());
        } else if (Types.MEMORY_SIZE_TYPE.equals(configProperty.getType())) {
            typeContent += " " + moreInformationAboutType(context, "memory-size-note-anchor", "MemorySize");
        }

        return typeContent;
    }

    @Override
    public String formatDefaultValue(ConfigProperty configProperty) {
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
                    return tooltip(defaultValue, javadocElement.get().description());
                }
            }
        }

        return "`" + defaultValue + "`";
    }

    @Override
    public int adjustedLevel(ConfigSection configSection, boolean multiRoot) {
        if (multiRoot) {
            return configSection.getLevel() + 1;
        }

        return configSection.getLevel();
    }

    @Override
    public String escapeCellContent(String value) {
        if (value == null) {
            return null;
        }

        return value.replace("|", "\\|");
    }

    @Override
    public String toAnchor(String value) {
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

    @Override
    public String formatSectionTitle(ConfigSection configSection) {
        Optional<JavadocElement> javadocElement = javadocRepository.getElement(configSection.getSourceClass(),
                configSection.getSourceName());

        if (javadocElement.isEmpty()) {
            throw new IllegalStateException(
                    "Couldn't find section title for: " + configSection.getSourceClass() + "#" + configSection.getSourceName());
        }

        String javadoc = JavadocTransformer.transform(javadocElement.get().description(), javadocElement.get().format(),
                javadocFormat());
        if (javadoc == null || javadoc.isBlank()) {
            throw new IllegalStateException(
                    "Couldn't find section title for: " + configSection.getSourceClass() + "#" + configSection.getSourceName());
        }

        return trimFinalDot(javadoc);
    }

    @Override
    public String formatName(Extension extension) {
        if (extension.name() == null) {
            return extension.artifactId();
        }

        return extension.name();
    }

    private static String trimFinalDot(String javadoc) {
        if (javadoc == null || javadoc.isBlank()) {
            return null;
        }

        javadoc = javadoc.trim();
        int dotIndex = javadoc.indexOf(".");

        if (dotIndex == -1) {
            return javadoc;
        }

        return javadoc.substring(0, dotIndex);
    }

    protected abstract JavadocFormat javadocFormat();

    protected abstract String moreInformationAboutType(Context context, String anchorRoot, String type);

    protected abstract String link(String href, String description);

    protected abstract String tooltip(String value, String javadocDescription);
}
