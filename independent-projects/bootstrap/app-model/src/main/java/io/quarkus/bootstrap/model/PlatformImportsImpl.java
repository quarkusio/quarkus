package io.quarkus.bootstrap.model;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PlatformImportsImpl implements PlatformImports, Serializable {

    public static final String PROPERTY_PREFIX = "platform.release-info@";

    public static final char PLATFORM_KEY_STREAM_SEPARATOR = '$';
    public static final char STREAM_VERSION_SEPARATOR = '#';

    private static int requiredIndex(String s, char c, int fromIndex) {
        final int i = s.indexOf(c, fromIndex);
        if (i < 0) {
            throw new IllegalArgumentException("Failed to locate '" + c + "' in '" + s + "'");
        }
        return i;
    }

    public static boolean isPlatformReleaseInfo(String s) {
        return s != null && s.startsWith(PROPERTY_PREFIX);
    }

    // metadata for each found platform release by platform key
    private final Map<String, PlatformInfo> allPlatformInfo = new HashMap<>();
    // imported platform BOMs by platform keys (groupId)
    private final Map<String, Collection<AppArtifactCoords>> importedPlatformBoms = new HashMap<>();

    private final Map<AppArtifactCoords, PlatformImport> platformImports = new HashMap<>();

    final Map<String, String> collectedProps = new HashMap<String, String>();
    private final Collection<AppArtifactCoords> platformBoms = new ArrayList<>();
    private final Collection<PlatformReleaseInfo> platformReleaseInfo = new ArrayList<>();

    public PlatformImportsImpl() {
    }

    public Collection<PlatformReleaseInfo> getPlatformReleaseInfo() {
        return platformReleaseInfo;
    }

    public Collection<AppArtifactCoords> getImportedPlatformBoms() {
        return platformBoms;
    }

    void addPlatformRelease(String propertyName, String propertyValue) {
        final int platformKeyStreamSep = requiredIndex(propertyName, PLATFORM_KEY_STREAM_SEPARATOR, PROPERTY_PREFIX.length());
        final int streamVersionSep = requiredIndex(propertyName, STREAM_VERSION_SEPARATOR, platformKeyStreamSep + 1);

        final String platformKey = propertyName.substring(PROPERTY_PREFIX.length(), platformKeyStreamSep);
        final String streamId = propertyName.substring(platformKeyStreamSep + 1, streamVersionSep);
        final String version = propertyName.substring(streamVersionSep + 1);
        allPlatformInfo.computeIfAbsent(platformKey, k -> new PlatformInfo(k)).getOrCreateStream(streamId).addIfNotPresent(
                version,
                () -> {
                    final PlatformReleaseInfo ri = new PlatformReleaseInfo(platformKey, streamId, version, propertyValue);
                    platformReleaseInfo.add(ri);
                    return ri;
                });
    }

    public void addPlatformDescriptor(String groupId, String artifactId, String classifier, String type, String version) {
        final AppArtifactCoords bomCoords = new AppArtifactCoords(groupId,
                artifactId.substring(0,
                        artifactId.length() - BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX.length()),
                null, "pom",
                version);
        platformImports.computeIfAbsent(bomCoords, c -> new PlatformImport()).descriptorFound = true;
        platformBoms.add(bomCoords);
    }

    public void addPlatformProperties(String groupId, String artifactId, String classifier, String type, String version,
            Path propsPath) throws AppModelResolverException {
        final AppArtifactCoords bomCoords = new AppArtifactCoords(groupId,
                artifactId.substring(0,
                        artifactId.length() - BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX.length()),
                null, "pom",
                version);
        platformImports.computeIfAbsent(bomCoords, c -> new PlatformImport());
        importedPlatformBoms.computeIfAbsent(groupId, g -> new ArrayList<>()).add(bomCoords);

        final Properties props = new Properties();
        try (InputStream is = Files.newInputStream(propsPath)) {
            props.load(is);
        } catch (IOException e) {
            throw new AppModelResolverException("Failed to read properties from " + propsPath, e);
        }
        for (Map.Entry<?, ?> prop : props.entrySet()) {
            final String name = String.valueOf(prop.getKey());
            if (name.startsWith(BootstrapConstants.PLATFORM_PROPERTY_PREFIX)) {
                if (isPlatformReleaseInfo(name)) {
                    addPlatformRelease(name, String.valueOf(prop.getValue()));
                } else {
                    collectedProps.putIfAbsent(name, String.valueOf(prop.getValue().toString()));
                }
            }
        }
    }

    public void setPlatformProperties(Map<String, String> platformProps) {
        this.collectedProps.putAll(platformProps);
    }

    @Override
    public Map<String, String> getPlatformProperties() {
        return collectedProps;
    }

    @Override
    public String getMisalignmentReport() {
        StringWriter error = null;
        for (Map.Entry<AppArtifactCoords, PlatformImport> pi : platformImports.entrySet()) {
            if (!pi.getValue().descriptorFound) {
                if (error == null) {
                    error = new StringWriter();
                    error.append(
                            "The Quarkus platform properties applied to the project are missing the corresponding Quarkus platform BOM imports: ");
                } else {
                    error.append(", ");
                }
                error.append(pi.getKey().toString());
            }
        }
        if (error != null) {
            return error.getBuffer().toString();
        }

        final Map<String, List<List<String>>> possibleAlignments = getPossibleAlignemnts(importedPlatformBoms);
        if (possibleAlignments.isEmpty()) {
            return null;
        }

        error = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(error)) {
            writer.append(
                    "Some of the imported Quarkus platform BOMs belong to different platform releases. To properly align the platform BOM imports, please, consider one of the following combinations:");
            writer.newLine();
            for (Map.Entry<String, List<List<String>>> entry : possibleAlignments.entrySet()) {
                writer.append("For platform ").append(entry.getKey()).append(':');
                writer.newLine();
                int i = 1;
                for (List<String> boms : entry.getValue()) {
                    writer.append("  ").append(String.valueOf(i++)).append(") ");
                    writer.newLine();
                    for (String bom : boms) {
                        writer.append(" - ").append(bom);
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return error.toString();
    }

    @Override
    public boolean isAligned() {
        return isAligned(importedPlatformBoms);
    }

    boolean isAligned(Map<String, Collection<AppArtifactCoords>> importedPlatformBoms) {
        for (Map.Entry<AppArtifactCoords, PlatformImport> pi : platformImports.entrySet()) {
            if (!pi.getValue().descriptorFound) {
                return false;
            }
        }
        for (Map.Entry<String, Collection<AppArtifactCoords>> platformImportedBoms : importedPlatformBoms.entrySet()) {
            final PlatformInfo platformInfo = allPlatformInfo.get(platformImportedBoms.getKey());
            if (platformInfo != null && !platformInfo.isAligned(platformImportedBoms.getValue())) {
                return false;
            }
        }
        return true;
    }

    private Map<String, List<List<String>>> getPossibleAlignemnts(
            Map<String, Collection<AppArtifactCoords>> importedPlatformBoms) {
        final Map<String, List<List<String>>> alignments = new HashMap<>(importedPlatformBoms.size());
        for (Map.Entry<String, Collection<AppArtifactCoords>> platformImportedBoms : importedPlatformBoms.entrySet()) {
            final PlatformInfo platformInfo = allPlatformInfo.get(platformImportedBoms.getKey());
            if (platformInfo == null || platformInfo.isAligned(platformImportedBoms.getValue())) {
                continue;
            }
            alignments.put(platformInfo.getPlatformKey(), platformInfo.getPossibleAlignments(platformImportedBoms.getValue()));
        }
        return alignments;
    }

    Collection<PlatformInfo> getPlatforms() {
        return allPlatformInfo.values();
    }

    PlatformInfo getPlatform(String platformKey) {
        return allPlatformInfo.get(platformKey);
    }

    private static class PlatformImport implements Serializable {
        boolean descriptorFound;
    }
}
