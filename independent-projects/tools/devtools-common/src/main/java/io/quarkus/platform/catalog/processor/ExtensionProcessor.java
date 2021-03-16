package io.quarkus.platform.catalog.processor;

import static io.quarkus.registry.catalog.Extension.*;

import io.quarkus.registry.catalog.Extension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        return of(extension).getShortName();
    }

    public static String getGuide(Extension extension) {
        return of(extension).getGuide();
    }

    public static String getCodestartName(Extension extension) {
        return of(extension).getCodestartName();
    }

    public static boolean isUnlisted(Extension extension) {
        return of(extension).isUnlisted();
    }

    public static List<String> getExtendedKeywords(Extension extension) {
        return of(extension).getExtendedKeywords();
    }

    public Extension getExtension() {
        return extension;
    }

    /**
     * @return string representing the location of primary guide for this extension.
     */
    public String getGuide() {
        return getMetadataValue(MD_GUIDE).asString();
    }

    public String getShortName() {
        final String shortName = getMetadataValue(MD_SHORT_NAME).asString();
        return shortName == null ? extension.getName() : shortName;
    }

    public String getCodestartName() {
        return getMetadataValue(MD_NESTED_CODESTART_NAME).asString();
    }

    public List<String> getCategories() {
        return getMetadataValue(MD_CATEGORIES).asStringList();
    }

    public List<String> getCodestartLanguages() {
        return getMetadataValue(MD_NESTED_CODESTART_LANGUAGES).asStringList();
    }

    public String getCodestartArtifact() {
        return getMetadataValue(MD_NESTED_CODESTART_ARTIFACT).asString();
    }

    public CodestartKind getCodestartKind() {
        return getMetadataValue(MD_NESTED_CODESTART_KIND).toEnum(CodestartKind.class);
    }

    public boolean providesExampleCode() {
        final CodestartKind codestartKind = getCodestartKind();
        return codestartKind != null && codestartKind.isExample();
    }

    public boolean isUnlisted() {
        return getMetadataValue(MD_UNLISTED).asBoolean();
    }

    public List<String> getKeywords() {
        return getMetadataValue(MD_KEYWORDS).asStringList();
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
        return ExtendedKeywords.extendsKeywords(this);
    }

    public List<String> getTags() {
        return getTags(null);
    }

    public List<String> getTags(String customStatusKey) {
        final List<String> keys = new ArrayList<>();
        keys.add(customStatusKey != null ? customStatusKey : MD_STATUS);
        final List<String> tags = keys.stream()
                .map(key -> getMetadataValue(key).asStringList())
                .flatMap(List::stream)
                .filter(tag -> !STABLE_STATS.equals(tag))
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(ArrayList::new));
        if (providesExampleCode()) {
            tags.add(PROVIDES_EXAMPLE_TAG);
        }
        return tags;
    }

    private Map<String, Object> getMetadata() {
        return extension.getMetadata();
    }

    private MetadataValue getMetadataValue(String mdGuide) {
        return MetadataValue.get(getMetadata(), mdGuide);
    }

}
