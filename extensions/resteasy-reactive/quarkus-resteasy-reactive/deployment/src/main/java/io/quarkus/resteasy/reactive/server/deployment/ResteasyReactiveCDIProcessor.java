package io.quarkus.resteasy.reactive.server.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.BeanParam;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.server.injection.ContextProducers;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.QuarkusContextProducers;
import io.quarkus.resteasy.reactive.spi.DynamicFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.JaxrsFeatureBuildItem;

public class ResteasyReactiveCDIProcessor {

    @BuildStep
    AutoInjectAnnotationBuildItem contextInjection(
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(ContextProducers.class, QuarkusContextProducers.class)
                        .build());
        return new AutoInjectAnnotationBuildItem(ResteasyReactiveServerDotNames.CONTEXT,
                DotName.createSimple(BeanParam.class.getName()));
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyReactiveDotNames.PATH, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyReactiveDotNames.APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyReactiveDotNames.PROVIDER,
                        BuiltinScope.SINGLETON.getName()));
    }

    // when an interface is annotated with @Path and there is only one implementation of it that is not annotated with @Path,
    // we need to make this class a bean. See https://github.com/quarkusio/quarkus/issues/15028
    @BuildStep
    void pathInterfaceImpls(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }
        ResourceScanningResult resourceScanningResult = resourceScanningResultBuildItem.get().getResult();
        Map<DotName, String> pathInterfaces = resourceScanningResult.getPathInterfaces();
        List<String> impls = new ArrayList<>();
        for (Map.Entry<DotName, String> i : pathInterfaces.entrySet()) {
            List<ClassInfo> candidateBeans = new ArrayList<>(1);
            for (ClassInfo clazz : resourceScanningResult.getIndex().getAllKnownImplementors(i.getKey())) {
                if (!Modifier.isAbstract(clazz.flags())) {
                    if ((clazz.enclosingClass() == null || Modifier.isStatic(clazz.flags())) &&
                            clazz.enclosingMethod() == null) {
                        candidateBeans.add(clazz);
                    }
                }
            }
            if (candidateBeans.size() == 1) {
                impls.add(candidateBeans.get(0).name().toString());
            }
        }
        if (!impls.isEmpty()) {
            additionalBeanBuildItemBuildProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClasses(impls.toArray(new String[0]))
                            .build());
        }
    }

    @BuildStep
    void additionalBeans(List<DynamicFeatureBuildItem> additionalDynamicFeatures,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<JaxrsFeatureBuildItem> featureBuildItems,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {

        AdditionalBeanBuildItem.Builder additionalProviders = AdditionalBeanBuildItem.builder();
        for (DynamicFeatureBuildItem dynamicFeature : additionalDynamicFeatures) {
            if (dynamicFeature.isRegisterAsBean()) {
                additionalProviders.addBeanClass(dynamicFeature.getClassName());
            } else {
                reflectiveClassBuildItemBuildProducer
                        .produce(new ReflectiveClassBuildItem(true, false, false, dynamicFeature.getClassName()));
            }
        }
        for (JaxrsFeatureBuildItem feature : featureBuildItems) {
            if (feature.isRegisterAsBean()) {
                additionalProviders.addBeanClass(feature.getClassName());
            } else {
                reflectiveClassBuildItemBuildProducer
                        .produce(new ReflectiveClassBuildItem(true, false, false, feature.getClassName()));
            }
        }
        additionalBean.produce(additionalProviders.setUnremovable().setDefaultScope(DotNames.SINGLETON).build());
    }

}
