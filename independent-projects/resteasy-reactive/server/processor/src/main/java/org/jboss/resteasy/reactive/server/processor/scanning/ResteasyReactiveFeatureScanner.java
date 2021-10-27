package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.model.ResourceDynamicFeature;
import org.jboss.resteasy.reactive.common.model.ResourceFeature;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.reflection.ReflectionBeanFactoryCreator;
import org.jboss.resteasy.reactive.server.model.DynamicFeatures;
import org.jboss.resteasy.reactive.server.model.Features;
import org.jboss.resteasy.reactive.spi.BeanFactory;

/**
 * Class that is responsible for scanning for features and dynamic features
 */
public class ResteasyReactiveFeatureScanner {

    /**
     * Creates a fully populated resource features instance, that are created via reflection.
     */
    public static Features createFeatures(IndexView indexView, ApplicationScanningResult result) {
        return createFeatures(indexView, result, new ReflectionBeanFactoryCreator());
    }

    /**
     * Creates a fully populated resource features instance, that are created via the provided factory creator
     */
    public static Features createFeatures(IndexView indexView, ApplicationScanningResult result,
            Function<String, BeanFactory<?>> factoryCreator) {
        Features features = new Features();
        for (String i : scanForFeatures(indexView, result)) {
            ResourceFeature resourceFeature = new ResourceFeature();
            resourceFeature.setFactory((BeanFactory<Feature>) factoryCreator.apply(i));
            features.addFeature(resourceFeature);
        }
        return features;
    }

    /**
     * Creates a fully populated resource dynamic features instance, that are created via reflection.
     */
    public static DynamicFeatures createDynamicFeatures(IndexView indexView, ApplicationScanningResult result) {
        return createDynamicFeatures(indexView, result, new ReflectionBeanFactoryCreator());
    }

    /**
     * Creates a fully populated resource features instance, that are created via the provided factory creator
     */
    public static DynamicFeatures createDynamicFeatures(IndexView indexView, ApplicationScanningResult result,
            Function<String, BeanFactory<?>> factoryCreator) {
        DynamicFeatures features = new DynamicFeatures();
        for (String i : scanForDynamicFeatures(indexView, result)) {
            ResourceDynamicFeature resourceFeature = new ResourceDynamicFeature();
            resourceFeature.setFactory((BeanFactory<DynamicFeature>) factoryCreator.apply(i));
            features.addFeature(resourceFeature);
        }
        return features;
    }

    public static Set<String> scanForFeatures(IndexView index, ApplicationScanningResult applicationScanningResult) {
        Collection<ClassInfo> features = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.FEATURE);
        Set<String> ret = new HashSet<>();
        for (ClassInfo featureClass : features) {
            ApplicationScanningResult.KeepProviderResult keepProviderResult = applicationScanningResult
                    .keepProvider(featureClass);
            if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
                ret.add(featureClass.name().toString());
            }
        }
        return ret;
    }

    public static Set<String> scanForDynamicFeatures(IndexView index, ApplicationScanningResult applicationScanningResult) {
        Collection<ClassInfo> features = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.DYNAMIC_FEATURE);
        Set<String> ret = new HashSet<>();
        for (ClassInfo featureClass : features) {
            ApplicationScanningResult.KeepProviderResult keepProviderResult = applicationScanningResult
                    .keepProvider(featureClass);
            if (keepProviderResult != ApplicationScanningResult.KeepProviderResult.DISCARD) {
                ret.add(featureClass.name().toString());
            }
        }
        return ret;
    }

}
