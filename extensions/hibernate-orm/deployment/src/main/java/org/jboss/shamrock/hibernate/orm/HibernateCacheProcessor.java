package org.jboss.shamrock.hibernate.orm;

import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;

/**
 * TODO: is this the correct place for this? Should the cache have its own extension?
 */
public final class HibernateCacheProcessor {

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        String[] classes = new String[] {
                "com.github.benmanes.caffeine.cache.SSA",
                "com.github.benmanes.caffeine.cache.SSMSA",
                "com.github.benmanes.caffeine.cache.PSA",
                "com.github.benmanes.caffeine.cache.PSW",
                "com.github.benmanes.caffeine.cache.PSWMS",
        };

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, classes));
    }

}
