package io.quarkus.kotlin.deployment;

import static io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem.builder;

import org.jboss.jandex.DotName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassFinalFieldsWritablePredicateBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;

public class KotlinProcessor {

    private static final String KOTLIN_JACKSON_MODULE = "com.fasterxml.jackson.module.kotlin.KotlinModule";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.KOTLIN);
    }

    /*
     * Register the Kotlin Jackson module if that has been added to the classpath
     * Producing the BuildItem is entirely safe since if quarkus-jackson is not on the classpath
     * the BuildItem will just be ignored
     */
    @BuildStep
    void registerKotlinJacksonModule(BuildProducer<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime(KOTLIN_JACKSON_MODULE)) {
            return;
        }

        classPathJacksonModules.produce(new ClassPathJacksonModuleBuildItem(KOTLIN_JACKSON_MODULE));
    }

    /**
     * Kotlin data classes that have multiple constructors need to have their final fields writable,
     * otherwise creating an instance of them with default values fails in native mode.
     */
    @BuildStep
    ReflectiveClassFinalFieldsWritablePredicateBuildItem dataClassPredicate() {
        return new ReflectiveClassFinalFieldsWritablePredicateBuildItem(new IsDataClassWithDefaultValuesPredicate());
    }

    /*
     * Register the Kotlin reflection types if they are present.
     */
    @BuildStep
    void registerKotlinReflection(final BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourcePatternsBuildItem> nativeResourcePatterns,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> reflectiveHierarchyIgnoreWarning) {

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("kotlin.reflect.jvm.internal.ReflectionFactoryImpl")
                .build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("kotlin.KotlinVersion").methods().fields().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("kotlin.KotlinVersion[]").constructors(false)
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("kotlin.KotlinVersion$Companion").constructors(false)
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("kotlin.KotlinVersion$Companion[]").constructors(false)
                .build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem
                        .builder("kotlin.collections.EmptyList", "kotlin.collections.EmptyMap", "kotlin.collections.EmptySet")
                        .build());

        nativeResourcePatterns.produce(builder().includePatterns(
                "META-INF/.*.kotlin_module$",
                "META-INF/services/kotlin.reflect.*",
                ".*.kotlin_builtins")
                .build());

        reflectiveHierarchyIgnoreWarning.produce(
                new ReflectiveHierarchyIgnoreWarningBuildItem(DotName.createSimple("kotlinx.serialization.KSerializer")));
        reflectiveHierarchyIgnoreWarning.produce(new ReflectiveHierarchyIgnoreWarningBuildItem(
                DotName.createSimple("kotlinx.serialization.descriptors.SerialDescriptor")));
    }
}
