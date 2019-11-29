package io.quarkus.kotlin.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;

public class KotlinProcessor {

    private static final String KOTLIN_JACKSON_MODULE = "com.fasterxml.jackson.module.kotlin.KotlinModule";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.KOTLIN);
    }

    /*
     * Register the Kotlin Jackson module if that has been added to the classpath
     * Producing the BuildItem is entirely safe since if quarkus-jackson is not on the classpath
     * the BuildItem will just be ignored
     */
    @BuildStep
    void registerKotlinJacksonModule(BuildProducer<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {
        try {
            Class.forName(KOTLIN_JACKSON_MODULE, false, Thread.currentThread().getContextClassLoader());
            classPathJacksonModules.produce(new ClassPathJacksonModuleBuildItem(KOTLIN_JACKSON_MODULE));
        } catch (Exception ignored) {
        }
    }
}
