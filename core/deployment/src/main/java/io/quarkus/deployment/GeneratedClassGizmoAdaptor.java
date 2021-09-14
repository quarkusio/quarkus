package io.quarkus.deployment;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;

public class GeneratedClassGizmoAdaptor implements ClassOutput {

    private final BuildProducer<GeneratedClassBuildItem> generatedClasses;
    private final Predicate<String> applicationClassPredicate;
    private final Map<String, StringWriter> sources;

    public GeneratedClassGizmoAdaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses, boolean applicationClass) {
        this(generatedClasses, new Predicate<String>() {
            @Override
            public boolean test(String t) {
                return applicationClass;
            }
        });
    }

    public GeneratedClassGizmoAdaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            Predicate<String> applicationClassPredicate) {
        this.generatedClasses = generatedClasses;
        this.applicationClassPredicate = applicationClassPredicate;
        this.sources = BootstrapDebug.DEBUG_SOURCES_DIR != null ? new ConcurrentHashMap<>() : null;
    }

    public GeneratedClassGizmoAdaptor(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            Function<String, String> generatedToBaseNameFunction) {
        this.generatedClasses = generatedClasses;
        this.applicationClassPredicate = new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return isApplicationClass(generatedToBaseNameFunction.apply(s));
            }
        };
        this.sources = BootstrapDebug.DEBUG_SOURCES_DIR != null ? new ConcurrentHashMap<>() : null;
    }

    @Override
    public void write(String className, byte[] bytes) {
        String source = null;
        if (sources != null) {
            StringWriter sw = sources.get(className);
            if (sw != null) {
                source = sw.toString();
            }
        }
        generatedClasses.produce(
                new GeneratedClassBuildItem(applicationClassPredicate.test(className), className, bytes, source));
    }

    @Override
    public Writer getSourceWriter(String className) {
        if (sources != null) {
            StringWriter writer = new StringWriter();
            sources.put(className, writer);
            return writer;
        }
        return ClassOutput.super.getSourceWriter(className);
    }

    public static boolean isApplicationClass(String className) {
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread()
                .getContextClassLoader();
        //if the class file is present in this (and not the parent) CL then it is an application class
        List<ClassPathElement> res = cl
                .getElementsWithResource(className.replace('.', '/') + ".class", true);
        return !res.isEmpty();
    }

}
