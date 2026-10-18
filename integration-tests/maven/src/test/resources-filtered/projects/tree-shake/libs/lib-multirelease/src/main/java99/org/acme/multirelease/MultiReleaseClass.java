package org.acme.multirelease;

/**
 * Version-99 variant of MultiReleaseClass. References FutureVersionOnly and
 * FutureVersionDep which don't exist in base or lower-version entries.
 * Since MultiReleaseClass is reachable from app code, these references must
 * be traced and preserved.
 */
public class MultiReleaseClass {
    private final FutureVersionOnly futureOnly = new FutureVersionOnly();

    public String version() {
        return futureOnly.version();
    }
}
