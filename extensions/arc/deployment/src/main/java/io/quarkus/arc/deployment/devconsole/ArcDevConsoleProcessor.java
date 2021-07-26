package io.quarkus.arc.deployment.devconsole;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InterceptorInfo;
import io.quarkus.arc.processor.ObserverInfo;
import io.quarkus.arc.runtime.ArcContainerSupplier;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.arc.runtime.BeanLookupSupplier;
import io.quarkus.arc.runtime.devconsole.EventsMonitor;
import io.quarkus.arc.runtime.devconsole.InvocationInterceptor;
import io.quarkus.arc.runtime.devconsole.InvocationTree;
import io.quarkus.arc.runtime.devconsole.InvocationsMonitor;
import io.quarkus.arc.runtime.devconsole.Monitored;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public class ArcDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    public DevConsoleRuntimeTemplateInfoBuildItem exposeArcContainer(ArcRecorder recorder) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("arcContainer",
                new ArcContainerSupplier());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void monitor(ArcConfig config, BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> runtimeInfos,
            BuildProducer<AdditionalBeanBuildItem> beans, BuildProducer<AnnotationsTransformerBuildItem> annotationTransformers,
            CustomScopeAnnotationsBuildItem customScopes,
            List<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        if (!config.devMode.monitoringEnabled) {
            return;
        }
        if (!config.transformUnproxyableClasses) {
            throw new IllegalStateException(
                    "Dev UI problem: monitoring of CDI business method invocations not possible\n\t- quarkus.arc.transform-unproxyable-classes was set to false and therefore it would not be possible to apply interceptors to unproxyable bean classes\n\t- please disable the monitoring feature via quarkus.arc.dev-mode.monitoring-enabled=false or enable unproxyable classes transformation");
        }
        // Events
        runtimeInfos.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("eventsMonitor",
                        new BeanLookupSupplier(EventsMonitor.class)));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(EventsMonitor.class));
        // Invocations
        beans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(InvocationTree.class, InvocationsMonitor.class, InvocationInterceptor.class,
                        Monitored.class)
                .build());
        Set<DotName> skipNames = new HashSet<>();
        skipNames.add(DotName.createSimple(InvocationTree.class.getName()));
        skipNames.add(DotName.createSimple(InvocationsMonitor.class.getName()));
        skipNames.add(DotName.createSimple(EventsMonitor.class.getName()));
        annotationTransformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public void transform(TransformationContext transformationContext) {
                if (transformationContext.isClass()) {
                    ClassInfo beanClass = transformationContext.getTarget().asClass();
                    if ((customScopes.isScopeDeclaredOn(beanClass)
                            || isAdditionalBeanDefiningAnnotationOn(beanClass, beanDefiningAnnotations))
                            && !skipNames.contains(beanClass.name())) {
                        transformationContext.transform().add(Monitored.class).done();
                    }
                }
            }
        }));
        runtimeInfos.produce(new DevConsoleRuntimeTemplateInfoBuildItem("invocationsMonitor",
                new BeanLookupSupplier(InvocationsMonitor.class)));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectBeanInfo(ValidationPhaseBuildItem validationPhaseBuildItem,
            CompletedApplicationClassPredicateBuildItem predicate) {
        BeanDeploymentValidator.ValidationContext validationContext = validationPhaseBuildItem.getContext();
        DevBeanInfos beanInfos = new DevBeanInfos();
        for (BeanInfo bean : validationContext.beans()) {
            beanInfos.addBean(DevBeanInfo.from(bean, predicate));
        }
        for (BeanInfo bean : validationContext.removedBeans()) {
            beanInfos.addRemovedBean(DevBeanInfo.from(bean, predicate));
        }
        for (ObserverInfo observer : validationContext.get(BuildExtension.Key.OBSERVERS)) {
            beanInfos.addObserver(DevObserverInfo.from(observer, predicate));
        }
        for (InterceptorInfo interceptor : validationContext.get(BuildExtension.Key.INTERCEPTORS)) {
            beanInfos.addInterceptor(DevInterceptorInfo.from(interceptor, predicate));
        }
        beanInfos.sort();
        return new DevConsoleTemplateInfoBuildItem("devBeanInfos", beanInfos);
    }

    private boolean isAdditionalBeanDefiningAnnotationOn(ClassInfo beanClass,
            List<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        for (BeanDefiningAnnotationBuildItem beanDefiningAnnotation : beanDefiningAnnotations) {
            if (beanClass.classAnnotation(beanDefiningAnnotation.getName()) != null) {
                return true;
            }
        }
        return false;
    }

}
