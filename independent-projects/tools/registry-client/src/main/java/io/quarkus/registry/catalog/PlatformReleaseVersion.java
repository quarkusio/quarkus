package io.quarkus.registry.catalog;

public interface PlatformReleaseVersion extends Comparable<PlatformReleaseVersion> {

    String getVersion();

    static PlatformReleaseVersion fromString(String s) {
        return PlatformReleaseVersionImpl.fromString(s);
    }
}
