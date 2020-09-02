package io.quarkus.resteasy.jaxb.deployment;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
import org.jboss.resteasy.annotations.providers.jaxb.WrappedMap;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class ResteasyJaxbProcessor {

    private static final List<Class<? extends Annotation>> RESTEASY_JAXB_ANNOTATIONS = Arrays.asList(
            Wrapped.class,
            WrappedMap.class);

    @BuildStep
    void processAnnotations(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        IndexView index = combinedIndexBuildItem.getIndex();

        // Handle RESTEasy annotations usage.
        for (Class annotationClazz : RESTEASY_JAXB_ANNOTATIONS) {
            DotName annotation = DotName.createSimple(annotationClazz.getName());

            if (!index.getAnnotations(annotation).isEmpty()) {
                addReflectiveClass(reflectiveClass, true, true, "org.jboss.resteasy.plugins.providers.jaxb.JaxbCollection");
                addReflectiveClass(reflectiveClass, true, true, "org.jboss.resteasy.plugins.providers.jaxb.JaxbMap");
                addReflectiveClass(reflectiveClass, true, true, "javax.xml.bind.annotation.W3CDomHandler");
                break;
            }
        }
    }

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_JAXB));
    }

    private void addReflectiveClass(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, boolean methods, boolean fields,
            String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, className));
    }
}
