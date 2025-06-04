package io.quarkus.platform.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.CatalogMergeUtility;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.selection.OriginPreference;
import io.quarkus.registry.util.PlatformArtifacts;

public class ToolsUtils {

    public static String requireProperty(String name) {
        final String value = getProperty(name);
        if (value == null) {
            throw new IllegalStateException("Failed to resolve required property " + name);
        }
        return value;
    }

    public static String getProperty(String name) {
        return getProperty(name, null);
    }

    public static String getProperty(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }

    public static Map<String, String> stringToMap(
            String str, String entrySeparator, String keyValueSeparator) {
        HashMap<String, String> result = new HashMap<>();
        for (String entry : StringUtils.splitByWholeSeparator(str, entrySeparator)) {
            String[] pair = StringUtils.splitByWholeSeparator(entry, keyValueSeparator, 2);

            if (pair.length > 0 && StringUtils.isBlank(pair[0])) {
                throw new IllegalArgumentException("Entry with empty key " + entry);
            }

            switch (pair.length) {
                case 1:
                    result.put(pair[0].trim(), "");
                    break;
                case 2:
                    result.put(pair[0].trim(), pair[1].trim());
                    break;
            }
        }

        return result;
    }

    public static boolean isNullOrEmpty(String arg) {
        return arg == null || arg.isEmpty();
    }

    public static String dotJoin(String... parts) {
        if (parts.length == 0) {
            return null;
        }
        if (parts.length == 1) {
            return parts[0];
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(parts[0]);
        int i = 1;
        while (i < parts.length) {
            buf.append('.').append(parts[i++]);
        }
        return buf.toString();
    }

    public static ExtensionCatalog resolvePlatformDescriptorDirectly(String bomGroupId, String bomArtifactId, String bomVersion,
            MavenArtifactResolver artifactResolver, MessageWriter log) {
        return resolvePlatformDescriptorDirectly(bomGroupId, bomArtifactId, bomVersion, artifactResolver, log, 1);
    }

    private static ExtensionCatalog resolvePlatformDescriptorDirectly(String bomGroupId, String bomArtifactId,
            String bomVersion,
            MavenArtifactResolver artifactResolver, MessageWriter log, int registryPreference) {
        if (bomVersion == null) {
            throw new IllegalArgumentException("BOM version was not provided");
        }
        Artifact catalogCoords = new DefaultArtifact(
                bomGroupId == null ? ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID : bomGroupId,
                (bomArtifactId == null ? ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID : bomArtifactId)
                        + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX,
                bomVersion, "json", bomVersion);

        Path platformJson = null;
        try {
            log.debug("Resolving platform descriptor %s", catalogCoords);
            platformJson = artifactResolver.resolve(catalogCoords).getArtifact().getFile().toPath();
        } catch (Exception e) {
            if (bomGroupId == null && catalogCoords.getArtifactId().startsWith("quarkus-bom")) {
                catalogCoords = new DefaultArtifact(
                        ToolsConstants.IO_QUARKUS,
                        "quarkus-bom" + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX,
                        catalogCoords.getClassifier(), catalogCoords.getExtension(), catalogCoords.getVersion());
                try {
                    log.debug("Resolving platform descriptor %s", catalogCoords);
                    platformJson = artifactResolver.resolve(catalogCoords).getArtifact().getFile().toPath();
                } catch (Exception e2) {
                }
            }
            if (platformJson == null) {
                final StringBuilder sb = new StringBuilder();
                sb.append("Failed to resolve extension catalog for ");
                sb.append(PlatformArtifacts.ensureBomArtifact(ArtifactCoords.of(catalogCoords.getGroupId(),
                        catalogCoords.getArtifactId(), catalogCoords.getClassifier(), catalogCoords.getExtension(),
                        catalogCoords.getVersion())).toCompactCoords());
                sb.append(
                        ". Make sure the groupId, artifactId and version are spelled correctly and the relevant Maven repositories are configured.");
                throw new RuntimeException(sb.toString(), e);
            }
        }
        ExtensionCatalog catalog;
        try {
            catalog = ExtensionCatalog.fromFile(platformJson);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize extension catalog " + platformJson, e);
        }
        Map<String, Object> md = catalog.getMetadata();
        if (md != null) {
            Object o = md.get("platform-release");
            if (o instanceof Map) {
                Object members = ((Map<?, ?>) o).get("members");
                if (members instanceof Collection) {
                    final Collection<?> memberList = (Collection<?>) members;
                    final List<ExtensionCatalog> catalogs = new ArrayList<>(memberList.size());

                    int memberIndex = 0;
                    for (Object m : memberList) {
                        if (!(m instanceof String)) {
                            continue;
                        }
                        final ExtensionCatalog memberCatalog;
                        if (catalog.getId().equals(m)) {
                            memberCatalog = catalog;
                        } else {
                            try {
                                final ArtifactCoords coords = ArtifactCoords.fromString((String) m);
                                catalogCoords = new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(),
                                        coords.getClassifier(), coords.getType(), coords.getVersion());
                                log.debug("Resolving platform descriptor %s", catalogCoords);
                                final Path jsonPath = artifactResolver.resolve(catalogCoords).getArtifact().getFile().toPath();
                                memberCatalog = ExtensionCatalog.fromFile(jsonPath);
                            } catch (Exception e) {
                                log.warn("Failed to resolve member catalog " + m, e);
                                continue;
                            }
                        }

                        final OriginPreference originPreference = new OriginPreference(registryPreference, 1, 1, ++memberIndex,
                                1);
                        Map<String, Object> metadata = new HashMap<>(memberCatalog.getMetadata());
                        metadata.put("origin-preference", originPreference);
                        ExtensionCatalog.Mutable mutableMemberCatalog = memberCatalog.mutable();
                        mutableMemberCatalog.setMetadata(metadata);
                        catalogs.add(mutableMemberCatalog.build());
                    }
                    catalog = CatalogMergeUtility.merge(catalogs);
                }
            }
        }
        if (catalog.getUpstreamQuarkusCoreVersion() != null
                && !catalog.getUpstreamQuarkusCoreVersion().isBlank()
                && !(bomVersion.equals(catalog.getUpstreamQuarkusCoreVersion())
                        && catalogCoords.getGroupId().equals(ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID)
                        && catalogCoords.getArtifactId().equals(ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID
                                + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX))) {
            try {
                final ExtensionCatalog upstreamCatalog = resolvePlatformDescriptorDirectly(null, null,
                        catalog.getUpstreamQuarkusCoreVersion(), artifactResolver, log, registryPreference + 1);
                catalog = CatalogMergeUtility.merge(List.of(catalog, upstreamCatalog));
            } catch (Exception e) {
                log.warn(e.getLocalizedMessage());
            }
        }
        return catalog;
    }

    public static ExtensionCatalog mergePlatforms(List<ArtifactCoords> platforms, MavenArtifactResolver artifactResolver) {
        // TODO remove this method once we have the registry service available
        return mergePlatforms(platforms, new BootstrapAppModelResolver(artifactResolver));
    }

    public static ExtensionCatalog mergePlatforms(List<ArtifactCoords> platforms, AppModelResolver artifactResolver) {
        // TODO remove this method once we have the registry service available
        List<ExtensionCatalog> catalogs = new ArrayList<>(platforms.size());
        for (ArtifactCoords platform : platforms) {
            final Path json;
            try {
                json = artifactResolver.resolve(ArtifactCoords.of(platform.getGroupId(), platform.getArtifactId(),
                        platform.getClassifier(), platform.getType(), platform.getVersion())).getResolvedPaths()
                        .getSinglePath();
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve platform descriptor " + platform, e);
            }
            try {
                catalogs.add(ExtensionCatalog.fromFile(json));
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize platform descriptor " + json, e);
            }
        }
        return CatalogMergeUtility.merge(catalogs);
    }

    @SuppressWarnings("unchecked")
    public static Properties readQuarkusProperties(ExtensionCatalog catalog) {
        Map<Object, Object> map = (Map<Object, Object>) catalog.getMetadata().getOrDefault("project", Collections.emptyMap());
        map = (Map<Object, Object>) map.getOrDefault("properties", Collections.emptyMap());
        final Properties properties = new Properties();
        map.entrySet().forEach(
                e -> properties.setProperty(e.getKey().toString(), e.getValue() == null ? null : e.getValue().toString()));
        return properties;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readProjectData(ExtensionCatalog catalog) {
        Map<Object, Object> map = (Map<Object, Object>) catalog.getMetadata().getOrDefault("project", Map.of());
        Map<String, Object> projectData = (Map<String, Object>) map.getOrDefault("codestart-data", Map.of());

        // fix the dockerfile entries for Platform < 3.23.1
        if (!projectData.containsKey("tooling-dockerfiles.dockerfile.jvm.from-template")) {
            projectData = projectData.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> {
                                if (!e.getKey().startsWith("dockerfile.")) {
                                    return e.getKey();
                                }
                                String key = e.getKey();
                                if ("dockerfile.native-micro".equals(key)) {
                                    key += ".from";
                                }

                                return "tooling-dockerfiles." + key;
                            },
                            Entry::getValue, (v1, v2) -> v1));
        }

        return projectData;
    }

    public static String requireProperty(Properties props, String name) {
        final String value = props.getProperty(name);
        if (value == null) {
            throw new IllegalStateException("Failed to resolve required property " + name);
        }
        return value;
    }

    public static String getMavenPluginArtifactId(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID);
    }

    public static String getMavenPluginGroupId(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_MAVEN_PLUGIN_GROUP_ID);
    }

    public static String getQuarkusCoreVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_CORE_VERSION);
    }

    public static String requireQuarkusCoreVersion(Properties props) {
        return requireProperty(props, ToolsConstants.PROP_QUARKUS_CORE_VERSION);
    }

    public static String getMavenPluginVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_MAVEN_PLUGIN_VERSION);
    }

    public static String getGradlePluginVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_GRADLE_PLUGIN_VERSION);
    }

    public static String getPluginKey(Properties props) {
        return getMavenPluginGroupId(props) + ":" + getMavenPluginArtifactId(props);
    }

    public static String getProposedMavenVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_PROPOSED_MVN_VERSION);
    }

    public static String getMavenWrapperVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_MVN_WRAPPER_VERSION);
    }

    public static String getGradleWrapperVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_GRADLE_WRAPPER_VERSION);
    }
}
