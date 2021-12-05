package io.quarkus.devtools.testing.registry.client;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryDescriptorConfig;
import io.quarkus.registry.config.RegistryNonPlatformExtensionsConfig;
import io.quarkus.registry.config.RegistryPlatformsConfig;
import io.quarkus.registry.config.RegistryQuarkusVersionsConfig;
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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

public class TestRegistryClientBuilder {

    private Path baseDir;
    private final RegistriesConfig.Mutable config = RegistriesConfig.builder();
    private final Map<String, TestRegistryBuilder> registries = new LinkedHashMap<>();

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
            config.persist(configYaml);
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
        private final RegistryConfig.Mutable config = RegistryConfig.builder();
        private final RegistryDescriptorConfig.Mutable descrConfig = RegistryDescriptorConfig.builder();
        private RegistryQuarkusVersionsConfig.Mutable quarkusVersions;
        private boolean external;
        private PlatformCatalog.Mutable platformCatalog;
        private PlatformCatalog.Mutable archivedPlatformCatalog;

        private List<TestPlatformCatalogMemberBuilder> memberCatalogs;
        private List<TestNonPlatformCatalogBuilder> nonPlatformCatalogs;
        private boolean enableMavenResolver;

        private TestRegistryBuilder(TestRegistryClientBuilder parent, String id) {
            this.parent = parent;
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
            config.setEnabled(false);
            return this;
        }

        public TestRegistryBuilder platformCatalog(PlatformCatalog newCatalog) {
            this.platformCatalog = newCatalog instanceof PlatformCatalog.Mutable
                    ? (PlatformCatalog.Mutable) newCatalog
                    : newCatalog.mutable();
            return this;
        }

        public TestPlatformCatalogPlatformBuilder newPlatform(String platformKey) {
            if (platformCatalog == null) {
                platformCatalog = PlatformCatalog.builder();
            }

            Platform.Mutable platform = (Platform.Mutable) platformCatalog.getPlatform(platformKey);
            if (platform == null) {
                platform = Platform.builder()
                        .setPlatformKey(platformKey);

                platformCatalog.addPlatform(platform);
            }

            return new TestPlatformCatalogPlatformBuilder(this, platform);
        }

        public TestNonPlatformCatalogBuilder newNonPlatformCatalog(String quarkusVersion) {
            final TestNonPlatformCatalogBuilder builder = new TestNonPlatformCatalogBuilder(this, quarkusVersion);
            if (nonPlatformCatalogs == null) {
                nonPlatformCatalogs = new ArrayList<>();
            }
            nonPlatformCatalogs.add(builder);
            return builder;
        }

        public TestRegistryBuilder recognizedQuarkusVersions(String expr) {
            return recognizedQuarkusVersions(expr, true);
        }

        public TestRegistryBuilder recognizedQuarkusVersions(String expr, boolean exclusiveProvider) {
            if (quarkusVersions == null) {
                quarkusVersions = RegistryQuarkusVersionsConfig.builder();
            }
            quarkusVersions.setRecognizedVersionsExpression(expr);
            quarkusVersions.setExclusiveProvider(exclusiveProvider);
            return this;
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
                config.setExtra("client-factory-url",
                        TestRegistryClient.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm());
                if (enableMavenResolver) {
                    config.setExtra("enable-maven-resolver", true);
                }
            }

            final RegistryConfig.Mutable registryConfig = RegistryConfig.builder();
            registryConfig.setId(config.getId());
            registryConfig.setDescriptor(descrConfig);
            if (quarkusVersions != null) {
                registryConfig.setQuarkusVersions(quarkusVersions);
            }

            final RegistryPlatformsConfig.Mutable platformConfig = RegistryPlatformsConfig.builder()
                    .setArtifact(new ArtifactCoords(registryGroupId, Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID,
                            null, "json", Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));

            if (platformCatalog == null && archivedPlatformCatalog == null) {
                platformConfig.setDisabled(true);
            } else {
                final Path platformsDir = getRegistryPlatformsDir(registryDir);
                final Map<String, PlatformCatalog.Mutable> platformsByQuarkusVersion = new HashMap<>();
                if (platformCatalog != null) {
                    persistPlatformCatalog(platformCatalog.build(), platformsDir);
                    for (Platform p : platformCatalog.getPlatforms()) {
                        for (PlatformStream s : p.getStreams()) {
                            for (PlatformRelease r : s.getReleases()) {
                                if (r.getQuarkusCoreVersion() == null) {
                                    throw new IllegalStateException(
                                            "Quarkus version has not be configured for platform release "
                                                    + p.getPlatformKey() + ":" + s.getId() + ":" + r.getVersion());
                                }
                                final PlatformCatalog.Mutable c = platformsByQuarkusVersion.computeIfAbsent(
                                        r.getQuarkusCoreVersion(),
                                        v -> PlatformCatalog.builder());
                                Platform.Mutable platform = (Platform.Mutable) c.getPlatform(p.getPlatformKey());
                                if (platform == null) {
                                    platform = Platform.builder()
                                            .setPlatformKey(p.getPlatformKey());
                                    c.addPlatform(platform);
                                }
                                PlatformStream.Mutable stream = (PlatformStream.Mutable) platform.getStream(s.getId());
                                if (stream == null) {
                                    stream = PlatformStream.builder()
                                            .setId(s.getId());
                                    platform.addStream(stream);
                                }
                                stream.addRelease(r);
                            }
                        }
                    }
                }

                if (archivedPlatformCatalog != null) {
                    for (Platform p : archivedPlatformCatalog.getPlatforms()) {
                        for (PlatformStream s : p.getStreams()) {
                            for (PlatformRelease r : s.getReleases()) {
                                if (r.getQuarkusCoreVersion() == null) {
                                    throw new IllegalStateException(
                                            "Quarkus version has not be configured for platform release "
                                                    + p.getPlatformKey() + ":" + s.getId() + ":" + r.getVersion());
                                }
                                final PlatformCatalog.Mutable c = platformsByQuarkusVersion.computeIfAbsent(
                                        r.getQuarkusCoreVersion(),
                                        v -> PlatformCatalog.builder());
                                Platform.Mutable platform = (Platform.Mutable) c.getPlatform(p.getPlatformKey());
                                if (platform == null) {
                                    platform = Platform.builder();
                                    platform.setPlatformKey(p.getPlatformKey());
                                    c.addPlatform(platform);
                                }
                                PlatformStream.Mutable stream = (PlatformStream.Mutable) platform.getStream(s.getId());
                                if (stream == null) {
                                    stream = PlatformStream.builder();
                                    stream.setId(s.getId());
                                    platform.addStream(stream);
                                }
                                stream.addRelease(r);
                            }
                        }
                    }
                }

                for (Map.Entry<String, PlatformCatalog.Mutable> entry : platformsByQuarkusVersion.entrySet()) {
                    persistPlatformCatalog(entry.getValue().build(), platformsDir.resolve(entry.getKey()));
                }

                if (memberCatalogs != null && !memberCatalogs.isEmpty()) {
                    platformConfig.setExtensionCatalogsIncluded(true);
                }
            }
            registryConfig.setPlatforms(platformConfig);

            final RegistryNonPlatformExtensionsConfig.Mutable nonPlatformConfig = RegistryNonPlatformExtensionsConfig.builder();

            nonPlatformConfig.setArtifact(getRegistryNonPlatformCatalogArtifact());
            if (nonPlatformCatalogs == null || nonPlatformCatalogs.isEmpty()) {
                nonPlatformConfig.setDisabled(true);
            } else {
                final Path nonPlatformDir = getRegistryNonPlatformDir(registryDir);
                for (TestNonPlatformCatalogBuilder nonPlatformCatalog : nonPlatformCatalogs) {
                    nonPlatformCatalog.persist(nonPlatformDir);
                }
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
                registryConfig.persist(descriptorJson);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to persist registry descriptor " + descriptorJson, e);
            }
        }

        private ArtifactCoords getRegistryNonPlatformCatalogArtifact() {
            return new ArtifactCoords(registryGroupId, Constants.DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID,
                    null, "json", Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION);
        }
    }

    public static class TestPlatformCatalogPlatformBuilder {

        private final TestRegistryBuilder registry;
        private final Platform.Mutable platform;

        private TestPlatformCatalogPlatformBuilder(TestRegistryBuilder registry, Platform.Mutable platform) {
            this.registry = registry;
            this.platform = platform;
        }

        public TestPlatformCatalogStreamBuilder newStream(String id) {
            final PlatformStream.Mutable stream = PlatformStream.builder()
                    .setId(id);

            platform.addStream(stream);
            return new TestPlatformCatalogStreamBuilder(this, stream);
        }

        public TestRegistryBuilder registry() {
            return registry;
        }
    }

    public static class TestPlatformCatalogStreamBuilder {

        private final TestPlatformCatalogPlatformBuilder platform;
        private final PlatformStream.Mutable stream;

        private TestPlatformCatalogStreamBuilder(TestPlatformCatalogPlatformBuilder platform, PlatformStream.Mutable stream) {
            this.platform = platform;
            this.stream = stream;
        }

        public TestPlatformCatalogReleaseBuilder newRelease(String version) {
            final PlatformRelease.Mutable release = PlatformRelease.builder()
                    .setVersion(PlatformReleaseVersion.fromString(version));

            stream.addRelease(release);
            return new TestPlatformCatalogReleaseBuilder(this, release);
        }

        public TestPlatformCatalogReleaseBuilder newArchivedRelease(String version) {
            final PlatformRelease.Mutable release = PlatformRelease.builder()
                    .setVersion(PlatformReleaseVersion.fromString(version));

            if (platform.registry.archivedPlatformCatalog == null) {
                platform.registry.archivedPlatformCatalog = PlatformCatalog.builder();
            }

            Platform.Mutable archivedPlatform = (Platform.Mutable) platform.registry.archivedPlatformCatalog
                    .getPlatform(platform.platform.getPlatformKey());
            if (archivedPlatform == null) {
                archivedPlatform = Platform.builder()
                        .setPlatformKey(platform.platform.getPlatformKey());
                platform.registry.archivedPlatformCatalog.addPlatform(archivedPlatform);
            }

            PlatformStream.Mutable archivedStream = (PlatformStream.Mutable) archivedPlatform
                    .getStream(stream.getId());
            if (archivedStream == null) {
                archivedStream = PlatformStream.builder()
                        .setId(stream.getId());
                archivedPlatform.addStream(archivedStream);
            }

            archivedStream.addRelease(release);
            return new TestPlatformCatalogReleaseBuilder(this, release);
        }

        public TestPlatformCatalogPlatformBuilder platform() {
            return platform;
        }
    }

    public static class TestPlatformCatalogReleaseBuilder {

        private final TestPlatformCatalogStreamBuilder stream;
        private final PlatformRelease.Mutable release;

        private TestPlatformCatalogReleaseBuilder(TestPlatformCatalogStreamBuilder stream, PlatformRelease.Mutable release) {
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
            if (release.getQuarkusCoreVersion() == null) {
                throw new RuntimeException("Quarkus core version hasn't been set");
            }
            final TestPlatformCatalogMemberBuilder quarkusBom = newMember("quarkus-bom");
            quarkusBom.addExtension("io.quarkus", "quarkus-core", release.getQuarkusCoreVersion());
            Map<String, Object> metadata = quarkusBom.extensions.getMetadata();
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
            release.getMemberBoms().add(bom);
        }

        @SuppressWarnings("unchecked")
        private void setReleaseInfo(ExtensionCatalog.Mutable catalog) {
            catalog.setQuarkusCoreVersion(release.getQuarkusCoreVersion());
            if (release.getUpstreamQuarkusCoreVersion() != null) {
                catalog.setUpstreamQuarkusCoreVersion(release.getUpstreamQuarkusCoreVersion());
            }

            Map<String, Object> metadata = catalog.getMetadata();
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
        private final ExtensionCatalog.Mutable extensions = ExtensionCatalog.builder();
        private final Model pom;

        private TestPlatformCatalogMemberBuilder(TestPlatformCatalogReleaseBuilder release, ArtifactCoords bom) {
            this.release = release;
            release.stream.platform.registry.addMemberCatalog(this);
            extensions.setBom(bom);
            extensions.setId(PlatformArtifacts.ensureCatalogArtifact(bom).toString());
            extensions.setPlatform(true);

            pom = new Model();
            pom.setModelVersion("4.0.0");
            pom.setGroupId(bom.getGroupId());
            pom.setArtifactId(bom.getArtifactId());
            pom.setVersion(bom.getVersion());
            pom.setPackaging("pom");
            pom.setDependencyManagement(new DependencyManagement());
        }

        public TestPlatformCatalogMemberBuilder addExtension(String artifactId) {
            return addExtension(extensions.getBom().getGroupId(), artifactId, extensions.getBom().getVersion());
        }

        public TestPlatformCatalogMemberBuilder addExtension(String groupId, String artifactId, String version) {
            final ArtifactCoords coords = new ArtifactCoords(groupId, artifactId, null, "jar", version);
            final Extension.Mutable e = Extension.builder()
                    .setArtifact(coords)
                    .setName(artifactId)
                    .setOrigins(Collections.singletonList(extensions));
            extensions.addExtension(e);

            final Dependency d = new Dependency();
            d.setGroupId(coords.getGroupId());
            d.setArtifactId(coords.getArtifactId());
            if (!coords.getClassifier().isBlank()) {
                d.setClassifier(coords.getClassifier());
            }
            if (!coords.getType().equals("jar")) {
                d.setType(coords.getType());
            }
            d.setVersion(coords.getVersion());
            pom.getDependencyManagement().addDependency(d);
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
                extensions.persist(json);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to persist extension catalog " + json, e);
            }

            Path pomPath = release.stream.platform.registry.parent.getMavenRepoDir();
            for (String s : pom.getGroupId().split("\\.")) {
                pomPath = pomPath.resolve(s);
            }
            pomPath = pomPath.resolve(pom.getArtifactId()).resolve(pom.getVersion());
            try {
                Files.createDirectories(pomPath);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directory " + pomPath, e);
            }
            pomPath = pomPath.resolve(pom.getArtifactId() + "-" + pom.getVersion() + ".pom");
            try {
                ModelUtils.persistModel(pomPath, pom);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to persist BOM at " + pomPath, e);
            }
        }
    }

    public static class TestNonPlatformCatalogBuilder {

        private final TestRegistryBuilder registry;
        private final ExtensionCatalog.Mutable extensions = ExtensionCatalog.builder();

        private TestNonPlatformCatalogBuilder(TestRegistryBuilder registry, String quarkusVersion) {
            this.registry = registry;
            final ArtifactCoords baseCoords = registry.getRegistryNonPlatformCatalogArtifact();
            extensions.setId(new ArtifactCoords(baseCoords.getGroupId(), baseCoords.getArtifactId(), quarkusVersion,
                    baseCoords.getType(), baseCoords.getVersion()).toString());
            extensions.setPlatform(false);
            extensions.setQuarkusCoreVersion(quarkusVersion);
            extensions.setBom(new ArtifactCoords("io.quarkus", "quarkus-bom", null, "pom", quarkusVersion));
        }

        public TestNonPlatformCatalogBuilder addExtension(String groupId, String artifactId, String version) {
            Extension e = Extension.builder()
                    .setArtifact(new ArtifactCoords(groupId, artifactId, null, "jar", version))
                    .setName(artifactId)
                    .setOrigins(Collections.singletonList(extensions));
            extensions.addExtension(e);
            return this;
        }

        public TestRegistryBuilder registry() {
            return registry;
        }

        private void persist(Path nonPlatformDir) {
            final Path json = getNonPlatformCatalogPath(nonPlatformDir, extensions.getQuarkusCoreVersion());
            try {
                extensions.persist(json);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to persist extension catalog " + json, e);
            }
        }
    }

    private static void persistPlatformCatalog(PlatformCatalog catalog, Path dir) {
        final Path platformsJson = dir.resolve("platforms.json");
        try {
            Files.createDirectories(dir);
            catalog.persist(platformsJson);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist platform catalog " + platformsJson, e);
        }
    }

    public static Path getRegistryMemberCatalogPath(Path registryDir, ArtifactCoords bom) {
        return getMemberCatalogPath(registryDir.resolve("members"), bom);
    }

    private static Path getMemberCatalogPath(Path memberDir, ArtifactCoords bom) {
        return memberDir.resolve(bom.getGroupId() + "." + bom.getArtifactId() + "." + bom.getVersion() + ".json");
    }

    static Path getRegistryNonPlatformCatalogPath(Path registryDir, String quarkusVersion) {
        return getNonPlatformCatalogPath(registryDir.resolve("non-platform"), quarkusVersion);
    }

    private static Path getNonPlatformCatalogPath(Path nonPlatformDir, String quarkusVersion) {
        return nonPlatformDir.resolve(quarkusVersion + ".json");
    }

    static Path getRegistryPlatformsCatalogPath(Path registryDir, String quarkusVersion) {
        return quarkusVersion == null ? getRegistryPlatformsDir(registryDir).resolve("platforms.json")
                : getRegistryPlatformsDir(registryDir).resolve(quarkusVersion).resolve("platforms.json");
    }

    private static Path getRegistryNonPlatformDir(Path registryDir) {
        return registryDir.resolve("non-platform");
    }

    private static Path getRegistryPlatformsDir(Path registryDir) {
        return registryDir.resolve("platforms");
    }

    static Path getRegistryDescriptorPath(Path registryDir) {
        return registryDir.resolve("config.json");
    }

    public static Path getRegistryDir(Path baseDir, String registryId) {
        return baseDir.resolve(registryId);
    }

    private Path getMavenRepoDir() {
        return getMavenRepoDir(baseDir);
    }

    public static Path getMavenRepoDir(Path baseDir) {
        return baseDir.resolve("maven-repo");
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
