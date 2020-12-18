
package io.quarkus.container.image.openshift.deployment;

import java.util.Optional;

import io.quarkus.container.image.deployment.util.ImageUtil;

public enum OpenshiftBaseNativeImage {

    //We only compare `repositories` so registries and tags are stripped
    QUARKUS("quarkus/ubi-quarkus-native-binary-s2i:latest", "/home/quarkus/", "application", "QUARKUS_HOME", "QUARKUS_OPTS");

    private final String image;
    private final String nativeBinaryDirectory;
    private final String fixedNativeBinaryName;
    private final String homeDirEnvVar;
    private final String optsEnvVar;

    public static Optional<OpenshiftBaseNativeImage> findMatching(String image) {
        for (OpenshiftBaseNativeImage candidate : OpenshiftBaseNativeImage.values()) {
            if (ImageUtil.getRepository(candidate.getImage()).equals(ImageUtil.getRepository(image))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private OpenshiftBaseNativeImage(String image, String nativeBinaryDirectory, String fixedNativeBinaryName,
            String homeDirEnvVar, String optsEnvVar) {
        this.image = image;
        this.nativeBinaryDirectory = nativeBinaryDirectory;
        this.fixedNativeBinaryName = fixedNativeBinaryName;
        this.homeDirEnvVar = homeDirEnvVar;
        this.optsEnvVar = optsEnvVar;
    }

    public String getImage() {
        return image;
    }

    public String getNativeBinaryDirectory() {
        return nativeBinaryDirectory;
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
