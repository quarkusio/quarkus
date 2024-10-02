package io.quarkus.test.junit;

import io.quarkus.deployment.pkg.steps.GraalVM;

public enum GraalVMVersion {
    GRAALVM_23_1_2(GraalVM.Version.VERSION_23_1_2),
    GRAALVM_23_1_3(GraalVM.Version.VERSION_23_1_3),
    GRAALVM_24_0_0(GraalVM.Version.VERSION_24_0_0),
    GRAALVM_24_0_999(GraalVM.Version.VERSION_24_0_999),
    GRAALVM_24_1_0(GraalVM.Version.VERSION_24_1_0);

    private final GraalVM.Version version;

    GraalVMVersion(GraalVM.Version version) {
        this.version = version;
    }

    public GraalVM.Version getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "GraalVMVersion{" +
                "version=" + version.getVersionAsString() +
                '}';
    }
}
