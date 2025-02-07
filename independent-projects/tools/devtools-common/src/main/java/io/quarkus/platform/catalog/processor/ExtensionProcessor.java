package io.quarkus.platform.catalog.processor;

import static io.quarkus.registry.catalog.Extension.MD_BUILT_WITH_QUARKUS_CORE;
import static io.quarkus.registry.catalog.Extension.MD_CATEGORIES;
import static io.quarkus.registry.catalog.Extension.MD_CLI_PLUGINS;
import static io.quarkus.registry.catalog.Extension.MD_GUIDE;
import static io.quarkus.registry.catalog.Extension.MD_KEYWORDS;
import static io.quarkus.registry.catalog.Extension.MD_MINIMUM_JAVA_VERSION;
import static io.quarkus.registry.catalog.Extension.MD_NESTED_CODESTART_ARTIFACT;
import static io.quarkus.registry.catalog.Extension.MD_NESTED_CODESTART_KIND;
import static io.quarkus.registry.catalog.Extension.MD_NESTED_CODESTART_LANGUAGES;
import static io.quarkus.registry.catalog.Extension.MD_NESTED_CODESTART_NAME;
import static io.quarkus.registry.catalog.Extension.MD_SHORT_NAME;
import static io.quarkus.registry.catalog.Extension.MD_UNLISTED;
import static java.util.stream.Collectors.toMap;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;

public final class ExtensionProcessor {

    private static final String QUARKUS_BOM_ARTIFACT_ID = "quarkus-bom";

    public enum CodestartKind {
        CORE,
        EXTENSION_CODESTART,
        EXAMPLE,
        SINGLETON_EXAMPLE;

        public boolean providesCode() {
            return this == EXTENSION_CODESTART || this == EXAMPLE || this == SINGLETON_EXAMPLE;
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
        return shortName == null ? "" : shortName;
    }

    public static String getGuide(Extension extension) {
        return getMetadataValue(extension, MD_GUIDE).asString();
    }

    public static Integer getMinimumJavaVersion(Extension extension) {
        return getMetadataValue(extension, MD_MINIMUM_JAVA_VERSION).asInteger();
    }

    public static List<String> getCategories(Extension extension) {
        return getMetadataValue(extension, MD_CATEGORIES).asStringList();
    }

    public static String getBuiltWithQuarkusCore(Extension extension) {
        return getMetadataValue(extension, MD_BUILT_WITH_QUARKUS_CORE).asString();
    }

    public static String getCodestartName(Extension extension) {
        return getMetadataValue(extension, MD_NESTED_CODESTART_NAME).asString();
    }

    public static Optional<ArtifactCoords> getBom(Extension extension) {
        if (extension == null || extension.getOrigins() == null || extension.getOrigins().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(extension.getOrigins().get(0).getBom());
    }

    public static Optional<ArtifactCoords> getNonQuarkusBomOnly(Extension extension) {
        return getBom(extension).filter(p -> !p.getArtifactId().equals(QUARKUS_BOM_ARTIFACT_ID));
    }

    public static List<String> getCodestartLanguages(Extension extension) {
        return getMetadataValue(extension, MD_NESTED_CODESTART_LANGUAGES).asStringList();
    }

    public static String getCodestartArtifact(Extension extension) {
        return getMetadataValue(extension, MD_NESTED_CODESTART_ARTIFACT).asString();
    }

    public static CodestartKind getCodestartKind(Extension extension) {
        if (getCodestartName(extension) == null) {
            return null;
        }
        return getMetadataValue(extension, MD_NESTED_CODESTART_KIND).toEnum(CodestartKind.class,
                CodestartKind.EXTENSION_CODESTART);
    }

    public static boolean providesCode(Extension extension) {
        final CodestartKind codestartKind = getCodestartKind(extension);
        return codestartKind != null && codestartKind.providesCode();
    }

    public static boolean isUnlisted(Extension extension) {
        return getMetadataValue(extension, MD_UNLISTED).asBoolean();
    }

    public static List<String> getKeywords(Extension extension) {
        return getMetadataValue(extension, MD_KEYWORDS).asStringList();
    }

    public static Set<String> getCliPlugins(Extension extension) {
        return new HashSet<>(getMetadataValue(extension, MD_CLI_PLUGINS).asStringList());
    }

    /**
     * List of strings to use for optimised word matching.
     * <br/>
     * <br/>
     * It includes a mix of static optimised keywords gathered from: the artifactId, name, shortname, categories and keywords
     * extracted from the
     * description
     *
     * @return list of keywords to use for matching.
     */
    public static Set<String> getExtendedKeywords(Extension extension) {
        return ExtendedKeywords.extendsKeywords(extension.getArtifact().getArtifactId(), extension.getName(),
                getShortName(extension), getCategories(extension), extension.getDescription(),
                getKeywords(extension));
    }

    /**
     * Clean version of the metadata with a Map of key:values to ease client usage
     *
     * @param extension
     * @return
     */
    public static Map<String, Collection<String>> getSyntheticMetadata(Extension extension) {
        Map<String, Object> extendedMetadata = new HashMap<>(extension.getMetadata());
        List<String> withList = new ArrayList<>(getMetadataValue(extension, "with").asStringList());
        if (providesCode(extension)) {
            withList.add("starter-code");
        }
        extendedMetadata.put("with", withList);
        extendedMetadata.put("origin", extension.hasPlatformOrigin() ? "platform" : "other");
        if (getMetadataValue(extension, "status").isEmpty()) {
            extendedMetadata.put("status", "stable");
        }
        return extendedMetadata.entrySet().stream()
                .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), (new MetadataValue(e.getValue()).asStringList())))
                .filter(e -> !e.getValue().isEmpty())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Extension getExtension() {
        return extension;
    }

    public Optional<ArtifactCoords> getBom() {
        return ExtensionProcessor.getBom(extension);
    }

    public Optional<ArtifactCoords> getNonQuarkusBomOnly() {
        return ExtensionProcessor.getNonQuarkusBomOnly(extension);
    }

    public String getBuiltWithQuarkusCore() {
        return getBuiltWithQuarkusCore(extension);
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

    public boolean providesCode() {
        return providesCode(extension);
    }

    public boolean isUnlisted() {
        return isUnlisted(extension);
    }

    public List<String> getKeywords() {
        return getKeywords(extension);
    }

    public Integer getMinimumJavaVersion() {
        return getMinimumJavaVersion(extension);
    }

    /**
     * List of strings to use for matching.
     * <br/>
     * <br/>
     * It includes a mix of static optimised keywords gathered from: the artifactId, name, shortname, categories and keywords
     * extracted from the
     * description
     *
     * @return list of keywords to use for matching.
     */
    public Set<String> getExtendedKeywords() {
        return getExtendedKeywords(extension);
    }

    public Set<String> getCliPlugins() {
        return getCliPlugins(extension);
    }

    public Map<String, Collection<String>> getSyntheticMetadata() {
        return getSyntheticMetadata(extension);
    }

    public static MetadataValue getMetadataValue(Extension extension, String path) {
        return MetadataValue.get(extension.getMetadata(), path);
    }
}
