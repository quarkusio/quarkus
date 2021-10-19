package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry configurations are stored as a list with a somewhat sneaky structure:
 * a raw string is allowed, or a string that is a key for an object is allowed.
 *
 * @see Deserializer custom deserialization of a List of RegistryConfig
 * @see Builder for type conversion to produce final values
 * @see Builder#build ensures RegistryConfig definitions are complete
 */
@JsonDeserialize(builder = RegistryConfigImpl.Builder.class)
@JsonSerialize(using = RegistryConfigImpl.Serializer.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryConfigImpl implements RegistryConfig {
    private static RegistryConfig defaultRegistry;

    private final String id;
    private final boolean enabled;
    private final String updatePolicy;
    private final RegistryDescriptorConfig descriptor;
    private final RegistryPlatformsConfig platforms;
    private final RegistryNonPlatformExtensionsConfig nonPlatformExtensions;
    private final RegistryMavenConfig mavenConfig;
    private final RegistryQuarkusVersionsConfig versionsConfig;
    private final Map<String, Object> extra;

    private RegistryConfigImpl(Builder builder) {
        this.id = builder.id;
        this.enabled = builder.enabled;
        this.updatePolicy = builder.updatePolicy;
        this.descriptor = builder.descriptor;
        this.platforms = builder.platforms;
        this.nonPlatformExtensions = builder.nonPlatformExtensions;
        this.mavenConfig = builder.mavenConfig;
        this.versionsConfig = builder.versionsConfig;
        this.extra = Collections.unmodifiableMap(builder.extra);
    }

    @Override
    @JsonIgnore
    public String getId() {
        return id;
    }

    @Override
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = JsonBooleanTrueFilter.class)
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getUpdatePolicy() {
        return updatePolicy;
    }

    @Override
    public RegistryDescriptorConfig getDescriptor() {
        return descriptor;
    }

    @Override
    public RegistryPlatformsConfig getPlatforms() {
        return platforms;
    }

    @Override
    public RegistryNonPlatformExtensionsConfig getNonPlatformExtensions() {
        return nonPlatformExtensions;
    }

    @Override
    public RegistryMavenConfig getMaven() {
        return mavenConfig;
    }

    @Override
    public RegistryQuarkusVersionsConfig getQuarkusVersions() {
        return versionsConfig;
    }

    @Override
    public Map<String, Object> getExtra() {
        return extra;
    }

    /**
     * Serializer for RegistryConfig objects. Deals with entries that could
     * be a single string, or a string key for an object.
     */
    public static class Serializer extends JsonSerializer<RegistryConfig> {
        @Override
        public void serialize(RegistryConfig value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (onlyId(value)) {
                // write as number to avoid quotes
                gen.writeNumber(value.getId());
            } else {
                gen.writeStartObject();
                gen.writeObjectFieldStart(value.getId());
                if (!value.isEnabled()) {
                    gen.writeObjectField("enabled", value.isEnabled());
                }
                writeUnlessNull(gen, "update-policy", value.getUpdatePolicy());
                writeUnlessNullOrClass(gen, "descriptor", value.getDescriptor(), RegistryDescriptorConfigImpl.Generated.class);
                writeUnlessNull(gen, "platforms", value.getPlatforms());
                writeUnlessNull(gen, "non-platform-extensions", value.getNonPlatformExtensions());
                writeUnlessNull(gen, "maven", value.getMaven());
                writeUnlessNull(gen, "quarkus-versions", value.getQuarkusVersions());

                // Include any extra fields if there are any
                Map<String, Object> extra = value.getExtra();
                if (extra != null && !extra.isEmpty()) {
                    for (Map.Entry<?, ?> entry : extra.entrySet()) {
                        gen.writeObjectField(entry.getKey().toString(), entry.getValue());
                    }
                }
                gen.writeEndObject();
                gen.writeEndObject();
            }
        }

        <T> void writeUnlessNullOrClass(JsonGenerator gen, String fieldName, Object obj, Class<T> clazz)
                throws IOException {
            if (obj != null && !clazz.isInstance(obj)) {
                gen.writeObjectField(fieldName, obj);
            }
        }

        void writeUnlessNull(JsonGenerator gen, String fieldName, Object obj) throws IOException {
            if (obj != null) {
                gen.writeObjectField(fieldName, obj);
            }
        }

        boolean onlyId(RegistryConfig value) {
            if (value.getId().equals(Constants.DEFAULT_REGISTRY_ID)) {
                // We don't want to print out defaults, including details for the default registry
                return true;
            }
            return value.getMaven() == null
                    && value.isEnabled()
                    && (value.getDescriptor() == null
                            || value.getDescriptor() instanceof RegistryDescriptorConfigImpl.Generated)
                    && value.getNonPlatformExtensions() == null
                    && value.getPlatforms() == null
                    && value.getUpdatePolicy() == null
                    && value.getQuarkusVersions() == null
                    && (value.getExtra() == null || value.getExtra().isEmpty());
        }
    }

    /**
     * Deserializer for a list of RegistryConfig objects. Deals with entries that could
     * be a single string, or a string key for an object.
     */
    static class Deserializer extends JsonDeserializer<List<RegistryConfig>> {
        @Override
        public List<RegistryConfig> deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            List<RegistryConfig> registries = new ArrayList<>();
            for (Iterator<JsonNode> i = node.elements(); i.hasNext();) {
                JsonNode element = i.next();
                if (element.isObject()) {
                    String id = element.fieldNames().next();
                    ObjectNode value = (ObjectNode) element.get(id);
                    value.put("id", id);
                    registries.add(p.getCodec().treeToValue(value, RegistryConfigImpl.class));
                } else if (element.isValueNode() && element.isTextual()) {
                    registries.add(new Builder().withId(element.textValue()).build());
                } else {
                    ctxt.weirdStringException(element.toString(),
                            RegistryConfigImpl.class,
                            "Invalid registry config list");
                }
            }
            return registries;
        }
    }

    public static class JsonBooleanTrueFilter {
        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Boolean)) {
                return false;
            }
            return (Boolean) obj;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder implements RegistryConfig {
        protected String id;
        protected boolean enabled = true;
        protected String updatePolicy;
        protected RegistryDescriptorConfig descriptor;
        protected RegistryPlatformsConfig platforms;
        protected RegistryNonPlatformExtensionsConfig nonPlatformExtensions;
        protected RegistryMavenConfig mavenConfig;
        protected RegistryQuarkusVersionsConfig versionsConfig;
        protected Map<String, Object> extra;

        public Builder() {
        }

        @JsonIgnore
        public Builder(RegistryConfig config) {
            this.id = config.getId();
            this.updatePolicy = config.getUpdatePolicy();
            this.descriptor = config.getDescriptor();
            this.platforms = config.getPlatforms();
            this.nonPlatformExtensions = config.getNonPlatformExtensions();
            this.mavenConfig = config.getMaven();
            this.versionsConfig = config.getQuarkusVersions();
            this.extra = new HashMap<>();
            if (config.getExtra() != null) {
                this.extra.putAll(config.getExtra());
            }
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = JsonBooleanTrueFilter.class)
        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withUpdatePolicy(String updatePolicy) {
            this.updatePolicy = updatePolicy;
            return this;
        }

        @JsonDeserialize(as = RegistryDescriptorConfigImpl.class)
        public Builder withDescriptor(RegistryDescriptorConfig descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        @JsonDeserialize(as = RegistryPlatformsConfigImpl.class)
        public Builder withPlatforms(RegistryPlatformsConfig platforms) {
            this.platforms = platforms;
            return this;
        }

        @JsonDeserialize(as = RegistryNonPlatformExtensionsConfigImpl.class)
        public Builder withNonPlatformExtensions(RegistryNonPlatformExtensionsConfig nonPlatformExtensions) {
            this.nonPlatformExtensions = nonPlatformExtensions;
            return this;
        }

        @JsonDeserialize(as = RegistryMavenConfigImpl.class)
        public Builder withMaven(RegistryMavenConfig mavenConfig) {
            this.mavenConfig = mavenConfig;
            return this;
        }

        @JsonDeserialize(as = RegistryQuarkusVersionsConfigImpl.class)
        public Builder withQuarkusVersions(RegistryQuarkusVersionsConfig versionsConfig) {
            this.versionsConfig = versionsConfig;
            return this;
        }

        @JsonAnySetter
        public Builder withExtra(String name, Object value) {
            if (extra == null) {
                extra = new HashMap<>();
            }
            extra.put(name, value);
            return this;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public boolean isEnabled() {
            return this.enabled;
        }

        @Override
        public String getUpdatePolicy() {
            return this.updatePolicy;
        }

        @Override
        public RegistryDescriptorConfig getDescriptor() {
            return this.descriptor;
        }

        @Override
        public RegistryPlatformsConfig getPlatforms() {
            return this.platforms;
        }

        @Override
        public RegistryNonPlatformExtensionsConfig getNonPlatformExtensions() {
            return this.nonPlatformExtensions;
        }

        @Override
        public RegistryMavenConfig getMaven() {
            return this.mavenConfig;
        }

        @Override
        public RegistryQuarkusVersionsConfig getQuarkusVersions() {
            return this.versionsConfig;
        }

        @Override
        public Map<String, Object> getExtra() {
            return this.extra;
        }

        /**
         * TODO: review this against RegistriesConfigLocator.completeRequiredConfig
         */
        public RegistryConfigImpl build() {
            if (Constants.DEFAULT_REGISTRY_ID.equals(id)) {
                fillInFromDefaultRegistry();
            }
            if (descriptor == null || descriptor.getArtifact() == null) {
                descriptor = new RegistryDescriptorConfigImpl.Generated(repoIdToArtifact(this.id));
            }
            return new RegistryConfigImpl(this);
        }

        private void fillInFromDefaultRegistry() {
            // The default registry itself is also built. Keep references to the
            // global default tucked behind tests for missing bits (it doesn't have any)
            if (descriptor == null) {
                descriptor = getDefaultRegistry().getDescriptor();
            }
            if (platforms == null) {
                platforms = getDefaultRegistry().getPlatforms();
            }
            if (nonPlatformExtensions == null) {
                nonPlatformExtensions = getDefaultRegistry().getNonPlatformExtensions();
            }
            if (mavenConfig == null) {
                mavenConfig = getDefaultRegistry().getMaven();
            }
        }

        private ArtifactCoords repoIdToArtifact(String id) {
            final String[] parts = id.split("\\.");
            final StringBuilder buf = new StringBuilder(id.length());
            int i = parts.length;
            buf.append(parts[--i]);
            while (--i >= 0) {
                buf.append('.').append(parts[i]);
            }
            return new ArtifactCoords(buf.toString(), Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID,
                    null, Constants.JSON, Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegistryConfigImpl that = (RegistryConfigImpl) o;
        return enabled == that.enabled
                && Objects.equals(id, that.id)
                && Objects.equals(updatePolicy, that.updatePolicy)
                && Objects.equals(descriptor, that.descriptor)
                && Objects.equals(platforms, that.platforms)
                && Objects.equals(nonPlatformExtensions, that.nonPlatformExtensions)
                && Objects.equals(mavenConfig, that.mavenConfig)
                && Objects.equals(versionsConfig, that.versionsConfig)
                && Objects.equals(extra, that.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, enabled, updatePolicy, descriptor, platforms, nonPlatformExtensions, mavenConfig,
                versionsConfig, extra);
    }

    @Override
    public String toString() {
        return "BaseRegistryConfig{" +
                "id='" + id + '\'' +
                ", enabled=" + enabled +
                ", updatePolicy='" + updatePolicy + '\'' +
                ", descriptor=" + descriptor +
                ", platforms=" + platforms +
                ", nonPlatformExtensions=" + nonPlatformExtensions +
                ", mavenConfig=" + mavenConfig +
                ", versionsConfig=" + versionsConfig +
                ", extra=" + extra +
                '}';
    }

    /**
     * Package private.
     * 
     * @return An immutable instance describing the default registry
     * @see RegistriesConfigLocator#getDefaultRegistry() for public retrieval of the default registry
     * @see Builder#build() does some special things to expand the default registry
     */
    static final RegistryConfig getDefaultRegistry() {
        if (defaultRegistry == null) {
            return defaultRegistry = RegistryConfigImpl.builder()
                    .withId(Constants.DEFAULT_REGISTRY_ID)

                    .withDescriptor(RegistryDescriptorConfigImpl.builder()
                            .withArtifact(new ArtifactCoords(Constants.DEFAULT_REGISTRY_GROUP_ID,
                                    Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID, null,
                                    Constants.JSON, Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION))
                            .build())

                    .withMaven(RegistryMavenConfigImpl.builder()
                            .withRepository(RegistryMavenRepoConfigImpl.builder()
                                    .withId(Constants.DEFAULT_REGISTRY_ID)
                                    .withUrl(Constants.DEFAULT_REGISTRY_MAVEN_REPO_URL)
                                    .build())
                            .build())

                    .withPlatforms(RegistryPlatformsConfigImpl.builder()
                            .withArtifact(new ArtifactCoords(Constants.DEFAULT_REGISTRY_GROUP_ID,
                                    Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID, null, Constants.JSON,
                                    Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION))
                            .build())

                    .withNonPlatformExtensions(RegistryNonPlatformExtensionsConfigImpl.builder()
                            .withArtifact(new ArtifactCoords(Constants.DEFAULT_REGISTRY_GROUP_ID,
                                    Constants.DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID, null,
                                    Constants.JSON,
                                    Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION))
                            .build())

                    .build();
        }
        return defaultRegistry;
    }
}
