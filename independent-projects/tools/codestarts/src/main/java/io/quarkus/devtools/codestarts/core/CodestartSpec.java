package io.quarkus.devtools.codestarts.core;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.devtools.codestarts.CodestartType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CodestartSpec {

    private final String name;
    private final boolean isPreselected;
    private final String ref;
    private final CodestartType type;
    private final boolean isFallback;
    private final Set<String> tags;
    private final Map<String, String> outputStrategy;
    private final Map<String, LanguageSpec> languagesSpec;

    @JsonCreator
    public CodestartSpec(@JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "ref") String ref,
            @JsonProperty(value = "type") CodestartType type,
            @JsonProperty("fallback") boolean isFallback,
            @JsonProperty("preselected") boolean isPreselected,
            @JsonProperty("tags") Set<String> tags,
            @JsonProperty("output-strategy") Map<String, String> outputStrategy,
            @JsonProperty("language") Map<String, LanguageSpec> languagesSpec) {
        this.name = requireNonNull(name, "name is required");
        this.tags = tags != null ? tags : Collections.emptySet();
        this.ref = ref != null ? ref : name;
        this.type = type != null ? type : CodestartType.CODE;
        this.isFallback = isFallback;
        this.isPreselected = isPreselected;
        this.outputStrategy = outputStrategy != null ? outputStrategy : Collections.emptyMap();
        this.languagesSpec = languagesSpec != null ? languagesSpec : Collections.emptyMap();
    }

    public String getName() {
        return name;
    }

    public String getRef() {
        return ref;
    }

    public Set<String> getTags() {
        return tags;
    }

    public CodestartType getType() {
        return type;
    }

    public boolean isFallback() {
        return isFallback;
    }

    public boolean isPreselected() {
        return isPreselected;
    }

    public Map<String, String> getOutputStrategy() {
        return outputStrategy;
    }

    public Map<String, LanguageSpec> getLanguagesSpec() {
        return languagesSpec;
    }

    public static final class LanguageSpec {
        private final Map<String, Object> data;
        private final Map<String, Object> sharedData;
        private final List<CodestartDep> dependencies;
        private final List<CodestartDep> testDependencies;

        public LanguageSpec() {
            this(null, null, null, null);
        }

        @JsonCreator
        public LanguageSpec(@JsonProperty("data") Map<String, Object> data,
                @JsonProperty("shared-data") Map<String, Object> sharedData,
                @JsonProperty("dependencies") List<CodestartDep> dependencies,
                @JsonProperty("test-dependencies") List<CodestartDep> testDependencies) {
            this.data = data != null ? data : Collections.emptyMap();
            this.sharedData = sharedData != null ? sharedData : Collections.emptyMap();
            this.dependencies = dependencies != null ? dependencies : Collections.emptyList();
            this.testDependencies = testDependencies != null ? testDependencies : Collections.emptyList();
        }

        public Map<String, Object> getData() {
            return data;
        }

        public Map<String, Object> getSharedData() {
            return sharedData;
        }

        public List<CodestartDep> getDependencies() {
            return dependencies;
        }

        public List<CodestartDep> getTestDependencies() {
            return testDependencies;
        }
    }

    public static class CodestartDep extends HashMap<String, String> {

        private static final String GROUP_ID = "groupId";
        private static final String ARTIFACT_ID = "artifactId";
        private static final String VERSION = "version";
        private static final String FORMATTED_GAV = "formatted-gav";
        private static final String FORMATTED_GA = "formatted-ga";

        public CodestartDep() {
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public CodestartDep(final String expression) {
            final String[] split = expression.split(":");
            if (split.length < 2) {
                throw new IllegalArgumentException("Invalid CodestartDep expression: " + expression);
            }
            this.put(GROUP_ID, split[0]);
            this.put(ARTIFACT_ID, split[1]);

            if (split.length == 3) {
                this.put(VERSION, split[2]);
                this.put(FORMATTED_GA, split[0] + ":" + split[1]);
            } else {
                this.put(FORMATTED_GA, expression);
            }
            this.put(FORMATTED_GAV, expression);

        }

        public String getGroupId() {
            return this.get(GROUP_ID);
        }

        public String getArtifactId() {
            return this.get(ARTIFACT_ID);
        }

        public String getVersion() {
            return this.get(VERSION);
        }

        @Override
        public String toString() {
            return this.get(FORMATTED_GAV);
        }

        @Override
        public int hashCode() {
            return this.getGroupId().hashCode() + this.getArtifactId().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Map) {
                final Map map = (Map) o;
                return Objects.equals(this.getGroupId(), map.get(GROUP_ID)) &&
                        Objects.equals(this.getArtifactId(), map.get(ARTIFACT_ID));
            }
            return false;
        }
    }
}
