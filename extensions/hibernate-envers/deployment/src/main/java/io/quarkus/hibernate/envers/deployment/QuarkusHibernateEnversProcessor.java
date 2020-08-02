package io.quarkus.hibernate.envers.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public final class QuarkusHibernateEnversProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.HIBERNATE_ENVERS);
    }

    @BuildStep
    public void registerEnversReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.hibernate.envers.DefaultRevisionEntity"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, "org.hibernate.tuple.entity.DynamicMapEntityTuplizer"));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, "org.hibernate.tuple.component.DynamicMapComponentTuplizer"));
    }

}
