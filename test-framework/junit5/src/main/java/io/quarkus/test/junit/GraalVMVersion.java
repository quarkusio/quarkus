package io.quarkus.test.junit;

public enum GraalVMVersion {
    GRAALVM_21_0(org.graalvm.home.Version.create(21, 0)),
    GRAALVM_22_0(org.graalvm.home.Version.create(22, 0)),
    GRAALVM_22_1(org.graalvm.home.Version.create(22, 1));

    private final org.graalvm.home.Version version;

    GraalVMVersion(org.graalvm.home.Version version) {
        this.version = version;
    }

    public org.graalvm.home.Version getVersion() {
        return version;
    }

    /**
     * Compares this version with another GraalVM version
     *
     * @return {@code -1} if this version is older than the other version,
     *         {@code +1} if it's newer and {@code 0} if they represent the same version
     */
    public int compareTo(org.graalvm.home.Version version) {
        return this.version.compareTo(version);
    }

    @Override
    public String toString() {
        return "GraalVMVersion{" +
                "version=" + version.toString() +
                '}';
    }
}
