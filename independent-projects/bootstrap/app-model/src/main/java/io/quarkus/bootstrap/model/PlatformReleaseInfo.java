package io.quarkus.bootstrap.model;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GACTV;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Platform release info that is encoded into a property in a platform properties artifact
 * following the format {@code platform.release-info@<platform-key>$<stream>#<version>=<bom-coords>(,<bom-coords>)}
 */
public class PlatformReleaseInfo implements Serializable {

    private final String platformKey;
    private final String stream;
    private final String version;
    private final List<ArtifactCoords> boms;

    public PlatformReleaseInfo(String platformKey, String stream, String version, String boms) {
        this.platformKey = platformKey;
        this.stream = stream;
        this.version = version;
        final String[] bomCoords = boms.split(",");
        this.boms = new ArrayList<>(bomCoords.length);
        for (String s : bomCoords) {
            this.boms.add(GACTV.fromString(s));
        }
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

    public String toString() {
        return getPropertyName() + '=' + getPropertyValue();
    }
}
