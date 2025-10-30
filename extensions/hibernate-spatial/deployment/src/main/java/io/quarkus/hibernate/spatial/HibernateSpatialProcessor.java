package io.quarkus.hibernate.spatial;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.hibernate.spatial.runtime.graal.DisableLoggingFeature;

@BuildSteps(onlyIf = HibernateSpatialEnabled.class)
public final class HibernateSpatialProcessor {

    @BuildStep
    public void registerSpatialReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // This is necessary because these classes are instantiated via reflection by geolatte-geom (when available)
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "org.geolatte.geom.codec.Sfa110WkbDecoder",
                "org.geolatte.geom.codec.Sfa121WkbDecoder",
                "org.geolatte.geom.codec.PostgisWkbDecoder",
                "org.geolatte.geom.codec.MySqlWkbDecoder",
                "org.geolatte.geom.codec.HANAWkbDecoder",
                "org.geolatte.geom.codec.Sfa110WkbEncoder",
                "org.geolatte.geom.codec.Sfa121WkbEncoder",
                "org.geolatte.geom.codec.PostgisWkbEncoder",
                "org.geolatte.geom.codec.PostgisWkbV2Encoder",
                "org.geolatte.geom.codec.MySqlWkbEncoder",
                "org.geolatte.geom.codec.HANAWkbEncoder")
                .reason(getClass().getName()).build());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    NativeImageFeatureBuildItem nativeImageFeature() {
        return new NativeImageFeatureBuildItem(DisableLoggingFeature.class);
    }
}
