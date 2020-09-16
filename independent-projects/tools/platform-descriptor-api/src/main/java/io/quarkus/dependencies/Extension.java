package io.quarkus.dependencies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 * @author <a href="http://kenfinnigan.me">Ken Finnigan</a>
 */
public class Extension implements Serializable {

    public static final String GROUP_ID = "group-id";

    public static final String ARTIFACT_ID = "artifact-id";

    public static final String VERSION = "version";

    public static final String MD_SHORT_NAME = "short-name";

    public static final String MD_CODESTART = "codestart";

    public static final String MD_GUIDE = "guide";

    /** Key used for keywords in metadata **/
    public static String MD_KEYWORDS = "keywords";

    public static final String MD_UNLISTED = "unlisted";

    public static final String MD_STATUS = "status";

    private String artifactId;
    private String groupId;
    private String scope;
    private String version;

    private String type;
    private String classifier;

    private String name;
    private String description;

    private String simplifiedArtifactId;
    private static final transient Pattern QUARKUS_PREFIX = Pattern.compile("^quarkus-");

    private Map<String, Object> metadata = new HashMap<String, Object>(3);

    public Extension() {
        // Use by mapper.
    }

    public Extension(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.setArtifactId(artifactId);
        this.version = version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Extension setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        this.simplifiedArtifactId = QUARKUS_PREFIX.matcher(artifactId).replaceFirst("");
        return this;
    }

    /** Group Id for the extension artifact */
    public String getGroupId() {
        return groupId;
    }

    public Extension setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public Extension setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Extension setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getType() {
        return type;
    }

    public Extension setType(String type) {
        this.type = type;
        return this;
    }

    public String getClassifier() {
        return classifier;
    }

    public Extension setClassifier(String classifier) {
        this.classifier = classifier;
        return this;
    }

    public String getName() {
        return name;
    }

    public Extension setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Extension setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Semi-Unstructured metadata used to provide metadata to tools and other
     * frontends.
     * 
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Extension setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public List<String> getKeywords() {
        List<String> kw = (List<String>) getMetadata().get(MD_KEYWORDS);
        return kw == null ? Collections.emptyList() : kw;
    }

    public Extension setKeywords(String[] keywords) {
        getMetadata().put(MD_KEYWORDS, Arrays.asList(keywords));
        return this;
    }

    /**
     * List of strings to use for matching.
     * 
     * Returns keywords + artifactid all in lowercase.
     * 
     * @return list of labels to use for matching.
     */
    public List<String> labelsForMatching() {
        List<String> list = new ArrayList<>();
        List<String> keywords = getKeywords();
        if (keywords != null) {
            list.addAll(keywords.stream().map(String::toLowerCase).collect(Collectors.toList()));
        }
        list.add(artifactId.toLowerCase());
        return list;
    }

    /**
     * Convert this Extension into a dependency
     * 
     * @param stripVersion if provided version will not be set on the Dependency
     * @return a Maven {@link Dependency} object
     */
    public Dependency toDependency(boolean stripVersion) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        if (scope != null && !scope.isEmpty()) {
            dependency.setScope(scope);
        }
        if (classifier != null && !classifier.isEmpty()) {
            dependency.setClassifier(classifier);
        }
        if (version != null && !version.isEmpty() && !stripVersion) {
            dependency.setVersion(version);
        }
        if (type != null && !type.isEmpty()) {
            dependency.setType(type);
        }
        return dependency;
    }

    public String managementKey() {
        return getGroupId() + ":" + getArtifactId();
    }

    public String gav() {
        return managementKey() + ":" + version;
    }

    public String getSimplifiedArtifactId() {
        return simplifiedArtifactId;
    }

    @Override
    public String toString() {
        return gav();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Extension extension = (Extension) o;
        return Objects.equals(artifactId, extension.artifactId) &&
                Objects.equals(groupId, extension.groupId) &&
                Objects.equals(version, extension.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactId, groupId, version);
    }

    public Extension setGuide(String guide) {
        getMetadata().put(MD_GUIDE, guide);
        return this;
    }

    /**
     * 
     * @return string representing the location of primary guide for this extension.
     */
    public String getGuide() {
        return (String) getMetadata().get(MD_GUIDE);
    }

    public String getShortName() {
        String shortName = (String) getMetadata().get(MD_SHORT_NAME);
        if (shortName == null) {
            return name;
        } else {
            return shortName;
        }
    }

    public Extension setShortName(String shortName) {
        getMetadata().put(MD_SHORT_NAME, shortName);
        return this;
    }

    public String getCodestart() {
        return (String) getMetadata().get(MD_CODESTART);
    }

    public Extension setCodestart(String codestart) {
        getMetadata().put(MD_CODESTART, codestart);
        return this;
    }

    public boolean isUnlisted() {
        Object val = getMetadata().get(MD_UNLISTED);
        if (val == null) {
            return false;
        } else if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }

        return false;
    }

    public void setUnlisted(boolean unlisted) {
        getMetadata().put(MD_UNLISTED, unlisted);
    }

    public Extension addMetadata(String key, Object value) {
        getMetadata().put(key, value);
        return this;

    }

    public Extension removeMetadata(String key) {
        getMetadata().remove(key);
        return this;
    }
}
