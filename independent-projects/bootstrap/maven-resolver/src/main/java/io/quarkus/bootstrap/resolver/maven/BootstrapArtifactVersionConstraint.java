package io.quarkus.bootstrap.resolver.maven;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionRange;

public class BootstrapArtifactVersionConstraint implements VersionConstraint {

    private final VersionRange range;

    private final Version version;

    /**
     * Creates a version constraint from the specified version range.
     *
     * @param range The version range, must not be {@code null}.
     */
    BootstrapArtifactVersionConstraint(VersionRange range) {
        this.range = requireNonNull(range, "version range cannot be null");
        this.version = null;
    }

    /**
     * Creates a version constraint from the specified version.
     *
     * @param version The version, must not be {@code null}.
     */
    BootstrapArtifactVersionConstraint(Version version) {
        this.version = requireNonNull(version, "version cannot be null");
        this.range = null;
    }

    public VersionRange getRange() {
        return range;
    }

    public Version getVersion() {
        return version;
    }

    public boolean containsVersion(Version version) {
        if (range == null) {
            return version.equals(this.version);
        } else {
            return range.containsVersion(version);
        }
    }

    @Override
    public String toString() {
        return String.valueOf((range == null) ? version : range);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }

        BootstrapArtifactVersionConstraint that = (BootstrapArtifactVersionConstraint) obj;

        return Objects.equals(range, that.range) && Objects.equals(version, that.getVersion());
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + hash(getRange());
        hash = hash * 31 + hash(getVersion());
        return hash;
    }

    private static int hash(Object obj) {
        return obj != null ? obj.hashCode() : 0;
    }

}
