package io.quarkus.devtools.testing.registry.client;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonPlatform;
import io.quarkus.registry.catalog.json.JsonPlatformCatalog;
import io.quarkus.registry.catalog.json.JsonPlatformRelease;
import io.quarkus.registry.catalog.json.JsonPlatformReleaseVersion;
import io.quarkus.registry.catalog.json.JsonPlatformStream;
import io.quarkus.registry.config.json.JsonRegistriesConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryDescriptorConfig;
import io.quarkus.registry.config.json.JsonRegistryNonPlatformExtensionsConfig;
import io.quarkus.registry.config.json.JsonRegistryPlatformsConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
import io.quarkus.registry.util.PlatformArtifacts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class TestRegistryClientBuilder {

    private Path baseDir;
    private JsonRegistriesConfig config = new JsonRegistriesConfig();
    private Map<String, TestRegistryBuilder> registries = new LinkedHashMap<>();

    public static TestRegistryClientBuilder newInstance() {
        return new TestRegistryClientBuilder();
    }

    private TestRegistryClientBuilder() {
    }

    public TestRegistryClientBuilder baseDir(Path baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public TestRegistryClientBuilder debug() {
        this.config.setDebug(true);
        return this;
    }

    public TestRegistryBuilder newRegistry(String id) {
        return registries.computeIfAbsent(id, i -> new TestRegistryBuilder(this, id));
    }

    public void build() {
        if (baseDir == null) {
            throw new IllegalStateException("The base directory has not been provided");
        }
        if (!Files.exists(baseDir)) {
            try {
                Files.createDirectories(baseDir);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directory " + baseDir, e);
            }
        } else if (!Files.isDirectory(baseDir)) {
            throw new IllegalStateException(baseDir + " exists and is not a directory");
        }

        for (TestRegistryBuilder registry : registries.values()) {
            configureRegistry(registry);
        }

        final Path configYaml = baseDir.resolve("config.yaml");
        try {
            RegistriesConfigMapperHelper.serialize(config, configYaml);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize registry client configuration " + configYaml, e);
        }
    }

    private void configureRegistry(TestRegistryBuilder registry) {
        registry.configure(getRegistryDir(baseDir, registry.config.getId()));
        config.addRegistry(registry.config);
    }

    public static class TestRegistryBuilder {

        private final TestRegistryClientBuilder parent;
        private final String registryGroupId;
        private JsonRegistryConfig config;
        private JsonRegistryDescriptorConfig descrConfig = new JsonRegistryDescriptorConfig();
        private boolean external;
        private PlatformCatalog platformCatalog;
        private List<JsonExtension> nonPlatformExtensions = new ArrayList<>(0);

        private List<TestPlatformCatalogMemberBuilder> memberCatalogs;
        private boolean enableMavenResolver;

        private TestRegistryBuilder(TestRegistryClientBuilder parent, String id) {
            this.parent = parent;
            this.config = new JsonRegistryConfig();
            config.setId(Objects.requireNonNull(id));

            registryGroupId = registryIdToGroupId(id);
            descrConfig.setArtifact(new ArtifactCoords(registryGroupId, Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID, null,
                    "json", Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
        }

        /**
         * Indicates that this registry is not a test registry.
         *
         * @return this instance
         */
        public TestRegistryBuilder external() {
            this.external = true;
            return this;
        }

        public TestRegistryBuilder disabled() {
            config.setDisabled(true);
            return this;
        }

        public TestRegistryBuilder platformCatalog(PlatformCatalog platformCatalog) {
            this.platformCatalog = platformCatalog;
            return this;
        }

        public TestPlatformCatalogPlatformBuilder newPlatform(String platformKey) {
            if (platformCatalog == null) {
                platformCatalog = new JsonPlatformCatalog();
            }
            JsonPlatform platform = (JsonPlatform) platformCatalog.getPlatform(platformKey);
            if (platform == null) {
                platform = new JsonPlatform();
                platform.setPlatformKey(platformKey);
            }
            ((JsonPlatformCatalog) platformCatalog).addPlatform(platform);
            final TestPlatformCatalogPlatformBuilder platformBuilder = new TestPlatformCatalogPlatformBuilder(this,
                    platform);
            return platformBuilder;
        }

        public TestRegistryClientBuilder clientBuilder() {
            return parent;
        }

        private void addMemberCatalog(TestPlatformCatalogMemberBuilder member) {
            if (this.memberCatalogs == null) {
                memberCatalogs = new ArrayList<>();
            }
            memberCatalogs.add(member);
        }

        private void configure(Path registryDir) {
            if (Files.exists(registryDir)) {
                if (!Files.isDirectory(registryDir)) {
                    throw new IllegalStateException(registryDir + " exists and is not a directory");
                }
                IoUtils.recursiveDelete(registryDir);
            }
            try {
                Files.createDirectories(registryDir);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directory " + registryDir, e);
            }

            if (!external) {
                config.setAny("client-factory-url",
                        TestRegistryClient.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm());
                if (enableMavenResolver) {
                    config.setAny("enable-maven-resolver", true);
                }
            }

            final JsonRegistryConfig registryConfig = new JsonRegistryConfig();
            registryConfig.setId(config.getId());
            registryConfig.setDescriptor(descrConfig);

            final JsonRegistryPlatformsConfig platformConfig = new JsonRegistryPlatformsConfig();
            platformConfig
                    .setArtifact(new ArtifactCoords(registryGroupId, Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID,
                            null, "json", Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
            if (platformCatalog == null) {
                platformConfig.setDisabled(true);
            } else {
                final Path platformsDir = getRegistryPlatformsDir(registryDir);
                persistPlatformCatalog(platformCatalog, platformsDir);
                final Map<String, JsonPlatformCatalog> platformsByQuarkusVersion = new HashMap<>();
                for (Platform p : platformCatalog.getPlatforms()) {
                    for (PlatformStream s : p.getStreams()) {
                        for (PlatformRelease r : s.getReleases()) {
                            if (r.getQuarkusCoreVersion() == null) {
                                throw new IllegalStateException("Quarkus version has not be configured for platform release "
                                        + p.getPlatformKey() + ":" + s.getId() + ":" + r.getVersion());
                            }
                            final JsonPlatformCatalog c = platformsByQuarkusVersion.computeIfAbsent(r.getQuarkusCoreVersion(),
                                    v -> new JsonPlatformCatalog());
                            JsonPlatform platform = (JsonPlatform) c.getPlatform(p.getPlatformKey());
                            if (platform == null) {
                                platform = new JsonPlatform();
                                platform.setPlatformKey(p.getPlatformKey());
                                c.addPlatform(platform);
                            }
                            JsonPlatformStream stream = (JsonPlatformStream) platform.getStream(s.getId());
                            if (stream == null) {
                                stream = new JsonPlatformStream();
                                stream.setId(s.getId());
                                platform.addStream(stream);
                            }
                            stream.addRelease(r);
                        }
                    }
                }
                for (Map.Entry<String, JsonPlatformCatalog> entry : platformsByQuarkusVersion.entrySet()) {
                    persistPlatformCatalog(entry.getValue(), platformsDir.resolve(entry.getKey()));
                }

                if (memberCatalogs != null && !memberCatalogs.isEmpty()) {
                    platformConfig.setExtensionCatalogsIncluded(true);
                }
            }
            registryConfig.setPlatforms(platformConfig);

            final JsonRegistryNonPlatformExtensionsConfig nonPlatformConfig = new JsonRegistryNonPlatformExtensionsConfig();
            nonPlatformConfig.setArtifact(
                    new ArtifactCoords(registryGroupId, Constants.DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID,
                            null, "json", Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
            if (nonPlatformExtensions.isEmpty()) {
                nonPlatformConfig.setDisabled(true);
            }
            registryConfig.setNonPlatformExtensions(nonPlatformConfig);

            if (memberCatalogs != null) {
                final Path membersDir = registryDir.resolve("members");
                try {
                    Files.createDirectories(membersDir);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to create directory " + membersDir, e);
                }
                for (TestPlatformCatalogMemberBuilder member : memberCatalogs) {
                    member.persist(membersDir);
                }
            }

            final Path descriptorJson = getRegistryDescriptorPath(registryDir);
            try {
                RegistriesConfigMapperHelper.serialize(registryConfig, descriptorJson);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to persist registry descriptor " + descriptorJson, e);
            }
        }
    }

    public static class TestPlatformCatalogPlatformBuilder {

        private final TestRegistryBuilder registry;
        private final JsonPlatform platform;

        private TestPlatformCatalogPlatformBuilder(TestRegistryBuilder registry, JsonPlatform platform) {
            this.registry = registry;
            this.platform = platform;
        }

        public TestPlatformCatalogStreamBuilder newStream(String id) {
            final JsonPlatformStream stream = new JsonPlatformStream();
            stream.setId(id);
            platform.addStream(stream);
            return new TestPlatformCatalogStreamBuilder(this, stream);
        }

        public TestRegistryBuilder registry() {
            return registry;
        }
    }

    public static class TestPlatformCatalogStreamBuilder {

        private final TestPlatformCatalogPlatformBuilder platform;
        private final JsonPlatformStream stream;

        private TestPlatformCatalogStreamBuilder(TestPlatformCatalogPlatformBuilder platform, JsonPlatformStream stream) {
            this.platform = platform;
            this.stream = stream;
        }

        public TestPlatformCatalogReleaseBuilder newRelease(String version) {
            final JsonPlatformRelease release = new JsonPlatformRelease();
            release.setVersion(JsonPlatformReleaseVersion.fromString(version));
            stream.addRelease(release);
            return new TestPlatformCatalogReleaseBuilder(this, release);
        }

        public TestPlatformCatalogPlatformBuilder platform() {
            return platform;
        }
    }

    public static class TestPlatformCatalogReleaseBuilder {

        private final TestPlatformCatalogStreamBuilder stream;
        private final JsonPlatformRelease release;

        private TestPlatformCatalogReleaseBuilder(TestPlatformCatalogStreamBuilder stream, JsonPlatformRelease release) {
            this.stream = stream;
            this.release = release;
        }

        public TestPlatformCatalogReleaseBuilder quarkusVersion(String quarkusVersion) {
            this.release.setQuarkusCoreVersion(quarkusVersion);
            return this;
        }

        public TestPlatformCatalogReleaseBuilder upstreamQuarkusVersion(String quarkusVersion) {
            this.release.setUpstreamQuarkusCoreVersion(quarkusVersion);
            return this;
        }

        public TestPlatformCatalogReleaseBuilder addMemberBom(ArtifactCoords bom) {
            addMemberBomInternal(bom);
            registry().enableMavenResolver = true;
            return this;
        }

        public TestPlatformCatalogMemberBuilder newMember(String artifactId) {
            final ArtifactCoords bom = new ArtifactCoords(stream.platform.platform.getPlatformKey(),
                    artifactId, null, "pom", release.getVersion().toString());
            addMemberBomInternal(bom);
            return new TestPlatformCatalogMemberBuilder(this, bom);
        }

        @SuppressWarnings("unchecked")
        public TestPlatformCatalogReleaseBuilder addCoreMember() {
            final TestPlatformCatalogMemberBuilder quarkusBom = newMember("quarkus-bom");
            quarkusBom.addExtension("quarkus-core");
            Map<String, Object> metadata = quarkusBom.extensions.getMetadata();
            if (metadata.isEmpty()) {
                metadata = new HashMap<>();
                quarkusBom.extensions.setMetadata(metadata);
            }
            metadata = (Map<String, Object>) metadata.computeIfAbsent("project", s -> new HashMap<>());
            metadata = (Map<String, Object>) metadata.computeIfAbsent("properties", s -> new HashMap<>());
            metadata.put("maven-plugin-groupId", quarkusBom.extensions.getBom().getGroupId());
            metadata.put("maven-plugin-artifactId", "quarkus-maven-plugin");
            metadata.put("maven-plugin-version", quarkusBom.extensions.getBom().getVersion());
            metadata.put("compiler-plugin-version", "3.8.1");
            metadata.put("surefire-plugin-version", "3.0.0-M5");
            return this;
        }

        public TestPlatformCatalogStreamBuilder stream() {
            return stream;
        }

        public TestRegistryBuilder registry() {
            return stream.platform.registry;
        }

        private void addMemberBomInternal(ArtifactCoords bom) {
            if (release.getMemberBoms().isEmpty()) {
                release.setMemberBoms(new ArrayList<>());
            }
            release.getMemberBoms().add(bom);
        }

        @SuppressWarnings("unchecked")
        private void setReleaseInfo(JsonExtensionCatalog catalog) {
            catalog.setQuarkusCoreVersion(release.getQuarkusCoreVersion());
            if (release.getUpstreamQuarkusCoreVersion() != null) {
                catalog.setUpstreamQuarkusCoreVersion(release.getUpstreamQuarkusCoreVersion());
            }

            Map<String, Object> metadata = catalog.getMetadata();
            if (metadata.isEmpty()) {
                metadata = new HashMap<>();
                catalog.setMetadata(metadata);
            }
            metadata = (Map<String, Object>) metadata.computeIfAbsent("platform-release", k -> new HashMap<>());
            metadata.put("platform-key", stream.platform.platform.getPlatformKey());
            metadata.put("stream", stream.stream.getId());
            metadata.put("version", release.getVersion());

            final List<ArtifactCoords> members = new ArrayList<>(release.getMemberBoms().size());
            for (ArtifactCoords coords : release.getMemberBoms()) {
                members.add(PlatformArtifacts.ensureCatalogArtifact(coords));
            }
            metadata.put("members", members);
        }
    }

    public static class TestPlatformCatalogMemberBuilder {

        private final TestPlatformCatalogReleaseBuilder release;
        private final JsonExtensionCatalog extensions = new JsonExtensionCatalog();

        private TestPlatformCatalogMemberBuilder(TestPlatformCatalogReleaseBuilder release, ArtifactCoords bom) {
            this.release = release;
            release.stream.platform.registry.addMemberCatalog(this);
            extensions.setBom(bom);
            extensions.setId(PlatformArtifacts.ensureCatalogArtifact(bom).toString());
            extensions.setPlatform(true);
        }

        public TestPlatformCatalogMemberBuilder addExtension(String artifactId) {
            final JsonExtension e = new JsonExtension();
            final ArtifactCoords bom = extensions.getBom();
            e.setArtifact(new ArtifactCoords(bom.getGroupId(), artifactId, null, "jar", bom.getVersion()));
            e.setName(artifactId);
            e.setOrigins(Collections.singletonList(extensions));
            extensions.addExtension(e);
            return this;
        }

        public TestPlatformCatalogReleaseBuilder release() {
            return release;
        }

        public TestRegistryBuilder registry() {
            return release.stream.platform.registry;
        }

        private void persist(Path memberDir) {
            release.setReleaseInfo(extensions);
            final ArtifactCoords bom = extensions.getBom();
            final Path json = getMemberCatalogPath(memberDir, bom);
            try {
                JsonCatalogMapperHelper.serialize(extensions, json);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to persist extension catalog " + json, e);
            }
        }
    }

    private static void persistPlatformCatalog(PlatformCatalog catalog, Path dir) {
        final Path platformsJson = dir.resolve("platforms.json");
        try {
            Files.createDirectories(dir);
            RegistriesConfigMapperHelper.serialize(catalog, platformsJson);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist platform catalog " + platformsJson, e);
        }
    }

    static Path getRegistryMemberCatalogPath(Path registryDir, ArtifactCoords bom) {
        return getMemberCatalogPath(registryDir.resolve("members"), bom);
    }

    private static Path getMemberCatalogPath(Path memberDir, ArtifactCoords bom) {
        return memberDir.resolve(bom.getGroupId() + "." + bom.getArtifactId() + "." + bom.getVersion() + ".json");
    }

    static Path getRegistryPlatformsCatalogPath(Path registryDir, String quarkusVersion) {
        return quarkusVersion == null ? getRegistryPlatformsDir(registryDir).resolve("platforms.json")
                : getRegistryPlatformsDir(registryDir).resolve(quarkusVersion).resolve("platforms.json");
    }

    private static Path getRegistryPlatformsDir(Path registryDir) {
        return registryDir.resolve("platforms");
    }

    static Path getRegistryDescriptorPath(Path registryDir) {
        return registryDir.resolve("config.json");
    }

    static Path getRegistryDir(Path baseDir, String registryId) {
        return baseDir.resolve(registryId);
    }

    private static String registryIdToGroupId(String id) {
        final String[] groupIdParts = id.split("\\.");
        if (groupIdParts.length == 1) {
            return groupIdParts[0];
        }
        final StringJoiner joiner = new StringJoiner(".");
        for (int i = groupIdParts.length - 1; i >= 0; i--) {
            joiner.add(groupIdParts[i]);
        }
        return joiner.toString();
    }
}
