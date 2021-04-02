package io.quarkus.platform.catalog.processor;

import static io.quarkus.registry.catalog.Extension.*;

import io.quarkus.registry.catalog.Extension;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ExtensionProcessor {

    private static final String STABLE_STATS = "stable";
    public static final String PROVIDES_EXAMPLE_TAG = "provides-example";

    public enum CodestartKind {
        CORE,
        EXAMPLE,
        SINGLETON_EXAMPLE;

        public boolean isExample() {
            return name().contains("EXAMPLE");
        }
    }

    private final Extension extension;

    private ExtensionProcessor(Extension extension) {
        this.extension = Objects.requireNonNull(extension);
    }

    public static ExtensionProcessor of(Extension extension) {
        return new ExtensionProcessor(extension);
    }

    public static String getShortName(Extension extension) {
        final String shortName = getMetadataValue(extension, MD_SHORT_NAME).asString();
        return shortName == null ? extension.getName() : shortName;
    }

    public static String getGuide(Extension extension) {
        return getMetadataValue(extension, MD_GUIDE).asString();
    }

    public static List<String> getCategories(Extension extension) {
        return getMetadataValue(extension, MD_CATEGORIES).asStringList();
    }

    public static String getCodestartName(Extension extension) {
        return getMetadataValue(extension, MD_NESTED_CODESTART_NAME).asString();
    }

    public static List<String> getCodestartLanguages(Extension extension) {
        return getMetadataValue(extension, MD_NESTED_CODESTART_LANGUAGES).asStringList();
    }

    public static String getCodestartArtifact(Extension extension) {
        return getMetadataValue(extension, MD_NESTED_CODESTART_ARTIFACT).asString();
    }

    public static CodestartKind getCodestartKind(Extension extension) {
        return getMetadataValue(extension, MD_NESTED_CODESTART_KIND).toEnum(CodestartKind.class);
    }

    public static boolean providesExampleCode(Extension extension) {
        final CodestartKind codestartKind = getCodestartKind(extension);
        return codestartKind != null && codestartKind.isExample();
    }

    public static boolean isUnlisted(Extension extension) {
        return getMetadataValue(extension, MD_UNLISTED).asBoolean();
    }

    public static List<String> getKeywords(Extension extension) {
        return getMetadataValue(extension, MD_KEYWORDS).asStringList();
    }

    /**
     * List of strings to use for matching.
     * <br/>
     * <br/>
     * It includes a mix of static keywords, the artifactId, and keywords extracted from the description
     *
     * @return list of keywords to use for matching.
     */
    public static List<String> getExtendedKeywords(Extension extension) {
        return ExtendedKeywords.extendsKeywords(extension.getArtifact().getArtifactId(), extension.getDescription(),
                getKeywords(extension));
    }

    public static List<String> getTags(Extension extension) {
        return getTags(extension, null);
    }

    public static List<String> getTags(Extension extension, String customStatusKey) {
        final List<String> keys = new ArrayList<>();
        keys.add(customStatusKey != null ? customStatusKey : MD_STATUS);
        final List<String> tags = keys.stream()
                .map(key -> getMetadataValue(extension, key).asStringList())
                .flatMap(List::stream)
                .filter(tag -> !STABLE_STATS.equals(tag))
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(ArrayList::new));
        if (providesExampleCode(extension)) {
            tags.add(PROVIDES_EXAMPLE_TAG);
        }
        return tags;
    }

    public Extension getExtension() {
        return extension;
    }

    /**
     * @return string representing the location of primary guide for this extension.
     */
    public String getGuide() {
        return getGuide(extension);
    }

    public String getShortName() {
        return getShortName(extension);
    }

    public String getCodestartName() {
        return getCodestartName(extension);
    }

    public List<String> getCategories() {
        return getCategories(extension);
    }

    public List<String> getCodestartLanguages() {
        return getCodestartLanguages(extension);
    }

    public String getCodestartArtifact() {
        return getCodestartArtifact(extension);
    }

    public CodestartKind getCodestartKind() {
        return getCodestartKind(extension);
    }

    public boolean providesExampleCode() {
        return providesExampleCode(extension);
    }

    public boolean isUnlisted() {
        return isUnlisted(extension);
    }

    public List<String> getKeywords() {
        return getKeywords(extension);
    }

    /**
     * List of strings to use for matching.
     * <br/>
     * <br/>
     * It includes a mix of static keywords, the artifactId, and keywords extracted from the description
     *
     * @return list of keywords to use for matching.
     */
    public List<String> getExtendedKeywords() {
        return getExtendedKeywords(extension);
    }

    public List<String> getTags() {
        return getTags(extension, null);
    }

    public List<String> getTags(String customStatusKey) {
        final List<String> keys = new ArrayList<>();
        keys.add(customStatusKey != null ? customStatusKey : MD_STATUS);
        final List<String> tags = keys.stream()
                .map(key -> getMetadataValue(extension, key).asStringList())
                .flatMap(List::stream)
                .filter(tag -> !STABLE_STATS.equals(tag))
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(ArrayList::new));
        if (providesExampleCode()) {
            tags.add(PROVIDES_EXAMPLE_TAG);
        }
        return tags;
    }

    public static MetadataValue getMetadataValue(Extension extension, String path) {
        return MetadataValue.get(extension.getMetadata(), path);
    }
}
