package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Platform release info that is encoded into a property in a platform properties artifact
 * following the format {@code platform.release-info@<platform-key>$<stream>#<version>=<bom-coords>(,<bom-coords>)}
 */
public class PlatformReleaseInfo implements Serializable, Mappable {

    static PlatformReleaseInfo fromMap(Map<String, Object> map) {
        final Collection<String> bomsStr = (Collection<String>) map.get(BootstrapConstants.MAPPABLE_BOMS);
        final List<ArtifactCoords> boms = new ArrayList<>(bomsStr.size());
        for (String bomStr : bomsStr) {
            boms.add(ArtifactCoords.fromString(bomStr));
        }
        return new PlatformReleaseInfo(
                (String) map.get(BootstrapConstants.MAPPABLE_PLATFORM_KEY),
                (String) map.get(BootstrapConstants.MAPPABLE_STREAM),
                (String) map.get(BootstrapConstants.MAPPABLE_VERSION),
                boms);
    }

    private static List<ArtifactCoords> parseArtifactCoords(String boms) {
        final String[] bomCoords = boms.split(",");
        final List<ArtifactCoords> result = new ArrayList<>(bomCoords.length);
        for (String s : bomCoords) {
            result.add(ArtifactCoords.fromString(s));
        }
        return result;
    }

    private static final long serialVersionUID = 7751600738849301644L;
    private final String platformKey;
    private final String stream;
    private final String version;
    private final List<ArtifactCoords> boms;

    public PlatformReleaseInfo(String platformKey, String stream, String version, String boms) {
        this(platformKey, stream, version, parseArtifactCoords(boms));
    }

    public PlatformReleaseInfo(String platformKey, String stream, String version, List<ArtifactCoords> boms) {
        this.platformKey = platformKey;
        this.stream = stream;
        this.version = version;
        this.boms = boms;
    }

    /**
     * The platform key. Could be the {@code groupId} of the stack, e.g. {@code io.quarkus.platform}
     *
     * @return platform key
     */
    public String getPlatformKey() {
        return platformKey;
    }

    /**
     * Platform stream. Could be the {@code major.minor} part of the platform release version.
     *
     * @return platform stream
     */
    public String getStream() {
        return stream;
    }

    /**
     * The version of the platform in a stream. Ideally, the micro version to make the comparisons easier.
     *
     * @return version in the stream
     */
    public String getVersion() {
        return version;
    }

    /**
     * Member BOM coordinates.
     *
     * @return member BOM coordinates
     */
    public List<ArtifactCoords> getBoms() {
        return boms;
    }

    String getPropertyName() {
        final StringBuilder buf = new StringBuilder();
        buf.append(PlatformImportsImpl.PROPERTY_PREFIX).append(platformKey)
                .append(PlatformImportsImpl.PLATFORM_KEY_STREAM_SEPARATOR)
                .append(stream)
                .append(PlatformImportsImpl.STREAM_VERSION_SEPARATOR).append(version);
        return buf.toString();
    }

    String getPropertyValue() {
        final StringBuilder buf = new StringBuilder();
        final List<ArtifactCoords> boms = getBoms();
        if (!boms.isEmpty()) {
            buf.append(boms.get(0).toString());
            for (int i = 1; i < boms.size(); ++i) {
                buf.append(',').append(boms.get(i));
            }
        }
        return buf.toString();
    }

    @Override
    public Map<String, Object> asMap(MappableCollectionFactory factory) {
        final Map<String, Object> map = factory.newMap(4);
        if (platformKey != null) {
            map.put(BootstrapConstants.MAPPABLE_PLATFORM_KEY, platformKey);
        }
        if (stream != null) {
            map.put(BootstrapConstants.MAPPABLE_STREAM, stream);
        }
        if (version != null) {
            map.put(BootstrapConstants.MAPPABLE_VERSION, version);
        }
        if (!boms.isEmpty()) {
            map.put(BootstrapConstants.MAPPABLE_BOMS,
                    Mappable.toStringCollection(boms, ArtifactCoords::toGACTVString, factory));
        }
        return map;
    }

    @Override
    public String toString() {
        return getPropertyName() + '=' + getPropertyValue();
    }
}
