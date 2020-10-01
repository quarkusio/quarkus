package io.quarkus.registry.catalog;

import io.quarkus.maven.ArtifactCoords;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface Extension {

    String MD_SHORT_NAME = "short-name";
    String MD_CODESTART = "codestart";
    String MD_GUIDE = "guide";
    /** Key used for keywords in metadata **/
    String MD_KEYWORDS = "keywords";
    String MD_UNLISTED = "unlisted";
    String MD_STATUS = "status";

    String getName();

    String getDescription();

    ArtifactCoords getArtifact();

    List<ExtensionOrigin> getOrigins();

    default boolean hasPlatformOrigin() {
        final List<ExtensionOrigin> origins = getOrigins();
        if (origins == null || origins.isEmpty()) {
            return false;
        }
        for (ExtensionOrigin o : origins) {
            if (o.isPlatform()) {
                return true;
            }
        }
        return false;
    }

    Map<String, Object> getMetadata();

    @SuppressWarnings("unchecked")
    default List<String> getKeywords() {
        List<String> kw = (List<String>) getMetadata().get(MD_KEYWORDS);
        return kw == null ? Collections.emptyList() : kw;
    }

    /**
     * List of strings to use for matching.
     *
     * Returns keywords + artifactid all in lowercase.
     *
     * @return list of labels to use for matching.
     */
    default List<String> labelsForMatching() {
        final List<String> keywords = getKeywords();
        final List<String> list = new ArrayList<>(1 + (keywords == null ? 0 : keywords.size()));
        if (keywords != null) {
            list.addAll(keywords.stream().map(String::toLowerCase).collect(Collectors.toList()));
        }
        list.add(getArtifact().getArtifactId().toLowerCase());
        return list;
    }

    /**
     *
     * @return string representing the location of primary guide for this extension.
     */
    default String getGuide() {
        return (String) getMetadata().get(MD_GUIDE);
    }

    default String managementKey() {
        final ArtifactCoords artifact = getArtifact();
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    default String getShortName() {
        final String shortName = (String) getMetadata().get(MD_SHORT_NAME);
        return shortName == null ? getName() : shortName;
    }

    default String getCodestart() {
        return (String) getMetadata().get(MD_CODESTART);
    }

    default boolean isUnlisted() {
        final Object val = getMetadata().get(MD_UNLISTED);
        if (val == null) {
            return false;
        }
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return false;
    }

}
