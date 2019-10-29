package io.quarkus.creator.phase.augment;

import java.util.List;

import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;

/**
 * Represents an outcome of {@link AugmentTask}
 *
 * @author Alexey Loubyansky
 */
public class AugmentOutcome {

    private final List<ArtifactResultBuildItem> packageOutput;
    private final JarBuildItem jar;
    private final NativeImageBuildItem nativeImage;

    public AugmentOutcome(List<ArtifactResultBuildItem> packageOutput, JarBuildItem thinJar,
            NativeImageBuildItem nativeImage) {
        this.packageOutput = packageOutput;
        this.jar = thinJar;
        this.nativeImage = nativeImage;
    }

    /**
     * The result of building the application
     */
    public List<ArtifactResultBuildItem> getPackageOutput() {
        return packageOutput;
    }

    public JarBuildItem getJar() {
        return jar;
    }

    public NativeImageBuildItem getNativeImage() {
        return nativeImage;
    }
}
