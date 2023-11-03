package io.quarkus.registry.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.json.JsonBooleanTrueFilter;
import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * <ul>
 * <li>Deserialization always uses the builder. {@link RegistryConfigImpl.BuilderDeserializer}
 * is used to create builders from lists containing a mix of raw strings or nested objects.</li>
 * <li>{@link RegistryConfigImpl.Serializer} is used as a serializer to suppress default
 * field values.</li>
 * </ul>
 *
 * @see RegistryConfig#builder() creates a builder
 * @see RegistryConfig#mutable() creates a builder from an existing RegistriesConfig
 * @see RegistryConfig#mutableFromFile(Path) creates a builder from the contents of a file
 * @see RegistryConfig#fromFile(Path) creates (and builds) a builder from the contents of a file
 * @see RegistryConfig#persist(Path) for writing an RegistriesConfig to a file
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
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
        this.updatePolicy = JsonBuilder.buildIfBuilder(builder.updatePolicy);
        this.descriptor = builder.descriptor == null || builder.descriptor.getArtifact() == null
                ? new RegistryDescriptorConfigImpl(repoIdToArtifact(id), true)
                : JsonBuilder.buildIfBuilder(builder.descriptor);
        this.platforms = JsonBuilder.buildIfBuilder(builder.platforms);
        this.nonPlatformExtensions = JsonBuilder.buildIfBuilder(builder.nonPlatformExtensions);
        this.mavenConfig = JsonBuilder.buildIfBuilder(builder.mavenConfig);
        this.versionsConfig = JsonBuilder.buildIfBuilder(builder.versionsConfig);
        this.extra = JsonBuilder.toUnmodifiableMap(builder.extra);
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
    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }

    private ArtifactCoords repoIdToArtifact(String id) {
        final String[] parts = id.split("\\.");
        final StringBuilder buf = new StringBuilder(id.length());
        int i = parts.length;
        buf.append(parts[--i]);
        while (--i >= 0) {
            buf.append('.').append(parts[i]);
        }
        return ArtifactCoords.of(buf.toString(), Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID,
                null, Constants.JSON, Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION);
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
        return this.getClass().getSimpleName() +
                "{id='" + id + '\'' +
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

    public static class Builder implements RegistryConfig.Mutable {
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

        Builder(String id) {
            this.id = id;
        }

        @JsonIgnore
        Builder(RegistryConfig config) {
            this.id = config.getId();
            this.updatePolicy = config.getUpdatePolicy();
            this.descriptor = config.getDescriptor();
            this.platforms = config.getPlatforms();
            this.nonPlatformExtensions = config.getNonPlatformExtensions();
            this.mavenConfig = config.getMaven();
            this.versionsConfig = config.getQuarkusVersions();

            this.extra = config.getExtra() == null
                    ? null
                    : new HashMap<>(config.getExtra());
        }

        @Override
        public String getId() {
            return this.id;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        @Override
        public boolean isEnabled() {
            return this.enabled;
        }

        @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = JsonBooleanTrueFilter.class)
        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @Override
        public String getUpdatePolicy() {
            return this.updatePolicy;
        }

        public Builder setUpdatePolicy(String updatePolicy) {
            this.updatePolicy = updatePolicy;
            return this;
        }

        @Override
        public RegistryDescriptorConfig getDescriptor() {
            return this.descriptor;
        }

        @JsonDeserialize(as = RegistryDescriptorConfigImpl.Builder.class)
        public Builder setDescriptor(RegistryDescriptorConfig descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        @Override
        public RegistryPlatformsConfig getPlatforms() {
            return this.platforms;
        }

        @JsonDeserialize(as = RegistryPlatformsConfigImpl.Builder.class)
        public Builder setPlatforms(RegistryPlatformsConfig platforms) {
            this.platforms = platforms;
            return this;
        }

        @Override
        public RegistryNonPlatformExtensionsConfig getNonPlatformExtensions() {
            return this.nonPlatformExtensions;
        }

        @JsonDeserialize(as = RegistryNonPlatformExtensionsConfigImpl.Builder.class)
        public Builder setNonPlatformExtensions(RegistryNonPlatformExtensionsConfig nonPlatformExtensions) {
            this.nonPlatformExtensions = nonPlatformExtensions;
            return this;
        }

        @Override
        public RegistryMavenConfig getMaven() {
            return this.mavenConfig;
        }

        @JsonDeserialize(as = RegistryMavenConfigImpl.Builder.class)
        public Builder setMaven(RegistryMavenConfig mavenConfig) {
            this.mavenConfig = mavenConfig;
            return this;
        }

        @Override
        public RegistryQuarkusVersionsConfig getQuarkusVersions() {
            return this.versionsConfig;
        }

        @JsonDeserialize(as = RegistryQuarkusVersionsConfigImpl.Builder.class)
        public Builder setQuarkusVersions(RegistryQuarkusVersionsConfig versionsConfig) {
            this.versionsConfig = versionsConfig;
            return this;
        }

        @Override
        public Map<String, Object> getExtra() {
            return extra == null ? Collections.emptyMap() : extra;
        }

        @Override
        public Mutable setExtra(Map<String, Object> newValues) {
            if (newValues != Collections.EMPTY_MAP) {
                this.extra = newValues;
            }
            return this;
        }

        @JsonAnySetter
        public Builder setExtra(String name, Object value) {
            if (extra == null) {
                extra = new HashMap<>();
            }
            extra.put(name, value);
            return this;
        }

        @Override
        public RegistryConfigImpl build() {
            if (Constants.DEFAULT_REGISTRY_ID.equals(id)) {
                fillInFromDefaultRegistry();
            }
            return new RegistryConfigImpl(this);
        }

        private void fillInFromDefaultRegistry() {
            // The default registry itself is also built (and so will hit this path).
            // Keep references to the global default tucked behind tests for missing
            // pieces, as the default registry will be complete.
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
    }

    /**
     * Serializer for RegistryConfig objects. Deals set entries that could
     * be a single string, or a string key for an object.
     */
    static class Serializer extends JsonSerializer<RegistryConfig> {
        @Override
        public void serialize(RegistryConfig value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            JsonStreamContext ctx = gen.getOutputContext();
            if (ctx.getParent() == null || ctx.getParent().inRoot()) {
                gen.writeStartObject();
                writeContents(value, gen);
                gen.writeEndObject();
            } else {
                if (onlyId(value)) {
                    gen.writeNumber(value.getId());
                } else {
                    gen.writeStartObject();
                    gen.writeObjectFieldStart(value.getId());
                    writeContents(value, gen);
                    gen.writeEndObject();
                    gen.writeEndObject();
                }
            }
        }

        static void writeContents(RegistryConfig value, JsonGenerator gen) throws IOException {
            if (!value.isEnabled()) {
                gen.writeObjectField("enabled", value.isEnabled());
            }
            writeUnlessNull(gen, "update-policy", value.getUpdatePolicy());
            writeUnlessNullOrTest(gen, "descriptor", value.getDescriptor(),
                    RegistryDescriptorConfigImpl::isGenerated);
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
        }

        static <T> void writeUnlessNullOrTest(JsonGenerator gen, String fieldName, T obj, Function<T, Boolean> test)
                throws IOException {
            if (obj == null || test.apply(obj)) {
                return;
            }
            gen.writeObjectField(fieldName, obj);
        }

        static void writeUnlessNull(JsonGenerator gen, String fieldName, Object obj) throws IOException {
            if (obj != null) {
                gen.writeObjectField(fieldName, obj);
            }
        }
    }

    static class BuilderDeserializer extends JsonDeserializer<RegistryConfigImpl.Builder> {
        @Override
        public RegistryConfigImpl.Builder deserialize(JsonParser p, DeserializationContext dctx)
                throws IOException {
            if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
                return new Builder().setId(p.getText());
            } else if (p.getCurrentToken() == JsonToken.START_OBJECT) {
                JsonStreamContext ctx = p.getParsingContext();
                final RegistryConfigImpl.Builder builder;
                if (ctx.getParent() == null || ctx.getParent().inRoot()) {
                    builder = p.readValueAs(RegistryConfigImpl.Builder.class);
                } else {
                    JsonBuilder.ensureNextToken(p, JsonToken.FIELD_NAME, dctx);
                    final String qerId = p.getCurrentName();
                    JsonBuilder.ensureNextToken(p, JsonToken.START_OBJECT, dctx);
                    builder = p.readValueAs(RegistryConfigImpl.Builder.class);
                    builder.setId(qerId);
                    JsonBuilder.ensureNextToken(p, JsonToken.END_OBJECT, dctx);
                }
                return builder;
            }
            return null;
        }
    }

    static boolean onlyId(RegistryConfig value) {
        if (value.getId().equals(Constants.DEFAULT_REGISTRY_ID)) {
            // We don't want to print out defaults, including details for the default registry
            return value.equals(getDefaultRegistry());
        }
        return value.getMaven() == null
                && value.isEnabled()
                && (value.getDescriptor() == null
                        || RegistryDescriptorConfigImpl.isGenerated(value.getDescriptor()))
                && value.getNonPlatformExtensions() == null
                && value.getPlatforms() == null
                && value.getUpdatePolicy() == null
                && value.getQuarkusVersions() == null
                && (value.getExtra() == null || value.getExtra().isEmpty());
    }

    static boolean isDefaultList(List<RegistryConfig> list) {
        return list.size() == 1
                && list.get(0) == RegistryConfig.defaultConfig();
    }

    /**
     * Package private.
     *
     * @return An immutable instance describing the default registry
     * @see RegistryConfig#defaultConfig() for public retrieval of the default registry
     * @see Builder#build() does some special things to fill-in the contents of the default
     *      registry when the id is read.
     */
    static RegistryConfig getDefaultRegistry() {
        if (defaultRegistry == null) {
            return defaultRegistry = RegistryConfig.builder()
                    .setId(Constants.DEFAULT_REGISTRY_ID)

                    .setDescriptor(RegistryDescriptorConfig.builder()
                            .setArtifact(ArtifactCoords.of(Constants.DEFAULT_REGISTRY_GROUP_ID,
                                    Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID, null,
                                    Constants.JSON, Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION))
                            .build())

                    .setMaven(RegistryMavenConfig.builder()
                            .setRepository(RegistryMavenRepoConfig.builder()
                                    .setId(Constants.DEFAULT_REGISTRY_ID)
                                    .setUrl(Constants.DEFAULT_REGISTRY_MAVEN_REPO_URL)
                                    .build())
                            .build())

                    .setPlatforms(RegistryPlatformsConfig.builder()
                            .setArtifact(ArtifactCoords.of(Constants.DEFAULT_REGISTRY_GROUP_ID,
                                    Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID, null, Constants.JSON,
                                    Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION))
                            .build())

                    .setNonPlatformExtensions(RegistryNonPlatformExtensionsConfig.builder()
                            .setArtifact(ArtifactCoords.of(Constants.DEFAULT_REGISTRY_GROUP_ID,
                                    Constants.DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID, null,
                                    Constants.JSON,
                                    Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION))
                            .build())

                    .build();
        }
        return defaultRegistry;
    }
}
