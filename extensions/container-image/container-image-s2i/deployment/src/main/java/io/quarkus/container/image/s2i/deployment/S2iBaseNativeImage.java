
package io.quarkus.container.image.s2i.deployment;

import java.util.Optional;

import io.quarkus.container.image.deployment.util.ImageUtil;

public enum S2iBaseNativeImage {

    //We only compare `repositories` so registries and tags are stripped
    QUARKUS("quarkus/ubi-quarkus-native-binary-s2i:latest", "application", "QUARKUS_HOME", "QUARKUS_OPTS");

    private final String image;
    private final String fixedNativeBinaryName;
    private final String homeDirEnvVar;
    private final String optsEnvVar;

    public static Optional<S2iBaseNativeImage> findMatching(String image) {
        for (S2iBaseNativeImage candidate : S2iBaseNativeImage.values()) {
            if (ImageUtil.getRepository(candidate.getImage()).equals(ImageUtil.getRepository(image))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private S2iBaseNativeImage(String image, String fixedNativeBinaryName, String homeDirEnvVar, String optsEnvVar) {
        this.image = image;
        this.fixedNativeBinaryName = fixedNativeBinaryName;
        this.homeDirEnvVar = homeDirEnvVar;
        this.optsEnvVar = optsEnvVar;
    }

    public String getImage() {
        return image;
    }

    public String getFixedNativeNinaryName() {
        return this.fixedNativeBinaryName;
    }

    public String getHomeDirEnvVar() {
        return homeDirEnvVar;
    }

    public String getOptsEnvVar() {
        return optsEnvVar;
    }
}
