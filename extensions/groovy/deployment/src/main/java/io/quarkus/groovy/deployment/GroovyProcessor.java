package io.quarkus.groovy.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import groovy.lang.Closure;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;

public class GroovyProcessor {

    private static final String GROOVY_JACKSON_MODULE = "com.fasterxml.jackson.module.groovy.GroovyModule";
    private static final String DGM_FORMAT_NAME = "org.codehaus.groovy.runtime.dgm$%d";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.GROOVY);
    }

    /*
     * Register the Groovy Jackson module if that has been added to the classpath
     * Producing the BuildItem is entirely safe since if quarkus-jackson is not on the classpath
     * the BuildItem will just be ignored
     */
    @BuildStep
    void registerGroovyJacksonModule(BuildProducer<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime(GROOVY_JACKSON_MODULE)) {
            return;
        }

        classPathJacksonModules.produce(new ClassPathJacksonModuleBuildItem(GROOVY_JACKSON_MODULE));
    }

    /*
     * Register the Groovy classes for reflection.
     */
    @BuildStep(onlyIf = NativeBuild.class)
    void registerGroovyReflection(final CombinedIndexBuildItem combinedIndex,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        IndexView index = combinedIndex.getIndex();
        // Register all closure classes
        String[] closureClasses = index.getAllKnownSubclasses(DotName.createSimple(Closure.class))
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(closureClasses).methods().build());
        // Register all DGM method classes
        List<String> dgmClassNames = new ArrayList<>();
        for (int i = 0;; i++) {
            String name = String.format(DGM_FORMAT_NAME, i);
            if (QuarkusClassLoader.isClassPresentAtRuntime(name)) {
                dgmClassNames.add(name);
                continue;
            }
            break;
        }
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(dgmClassNames.toArray(new String[0])).build());
    }

    @BuildStep(onlyIf = NativeBuild.class)
    List<NativeImageProxyDefinitionBuildItem> registerProxies() {
        // Register the main functional interfaces to implement them using Closures
        List<NativeImageProxyDefinitionBuildItem> proxies = new ArrayList<>();
        proxies.add(new NativeImageProxyDefinitionBuildItem(Function.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(Consumer.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(Predicate.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(Supplier.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(BiFunction.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(BiConsumer.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(BiPredicate.class.getName()));
        return proxies;
    }
}
