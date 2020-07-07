package io.quarkus.hibernate.orm.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class HibernateEnversProcessor {

    @BuildStep
    public void registerEnversReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.hibernate.envers.DefaultRevisionEntity"));
        simpleConstructor(reflectiveClass, org.hibernate.tuple.entity.DynamicMapEntityTuplizer.class);
        simpleConstructor(reflectiveClass, org.hibernate.tuple.component.DynamicMapComponentTuplizer.class);
    }

    /**
     * Register classes which we know will only need to be created via their no-arg constructor
     */
    private void simpleConstructor(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, final Class<?> clazz) {
        simpleConstructor(reflectiveClass, clazz.getName());
    }

    private void simpleConstructor(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, final String clazzName) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, clazzName));
    }

}
