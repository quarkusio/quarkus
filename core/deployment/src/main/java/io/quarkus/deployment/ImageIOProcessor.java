package io.quarkus.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class ImageIOProcessor {

    @BuildStep(onlyIf = NativeBuild.class)
    List<RuntimeInitializedClassBuildItem> runtimeInitImageIOClasses() {
        // The following classes hold instances of java.awt.color.ICC_ColorSpace that are not allowed in the image
        // heap as this class should be initialized at image runtime
        // (See https://github.com/quarkusio/quarkus/issues/12535)
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem("javax.imageio.ImageTypeSpecifier"),
                new RuntimeInitializedClassBuildItem("com.sun.imageio.plugins.jpeg.JPEG$JCS"),
                new RuntimeInitializedClassBuildItem("java.awt.image.AreaAveragingScaleFilter"));
    }
}
