package io.quarkus.creator.phase.augment;

import java.util.List;

import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.ThinJarBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarBuildItem;

/**
 * Represents an outcome of {@link AugmentTask}
 *
 * @author Alexey Loubyansky
 */
public class AugmentOutcome {

    private final List<ArtifactResultBuildItem> packageOutput;
    private final ThinJarBuildItem thinJar;
    private final UberJarBuildItem uberJar;
    private final NativeImageBuildItem nativeImage;

    public AugmentOutcome(List<ArtifactResultBuildItem> packageOutput, ThinJarBuildItem thinJar, UberJarBuildItem uberJar,
            NativeImageBuildItem nativeImage) {
        this.packageOutput = packageOutput;
        this.thinJar = thinJar;
        this.uberJar = uberJar;
        this.nativeImage = nativeImage;
    }

    /**
     * The result of building the application
     */
    public List<ArtifactResultBuildItem> getPackageOutput() {
        return packageOutput;
    }

    public ThinJarBuildItem getThinJar() {
        return thinJar;
    }

    public UberJarBuildItem getUberJar() {
        return uberJar;
    }

    public NativeImageBuildItem getNativeImage() {
        return nativeImage;
    }
}
