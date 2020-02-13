package io.quarkus.hibernate.orm.deployment;

import antlr.CommonToken;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * This list of classes which any Hibernate ORM using application should register for reflective access in native mode.
 * <p>
 * Most of these are statically provided by the dependency hibernate-graalvm; this file exists to be able to easily
 * add new entries without needing to wait for an Hibernate ORM release.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class HibernateOrmReflections {

    @BuildStep
    public void registerCoreReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        simpleConstructor(reflectiveClass, CommonToken.class);
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
