package io.quarkus.arc.deployment.devconsole;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.devconsole.DependencyGraph.Link;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.DecoratorInfo;
import io.quarkus.arc.processor.InjectionPointInfo;
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
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public class ArcDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    public DevConsoleRuntimeTemplateInfoBuildItem exposeArcContainer(ArcRecorder recorder,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("arcContainer",
                new ArcContainerSupplier(), this.getClass(), curateOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void monitor(ArcConfig config, BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> runtimeInfos,
            BuildProducer<AdditionalBeanBuildItem> beans, BuildProducer<AnnotationsTransformerBuildItem> annotationTransformers,
            CustomScopeAnnotationsBuildItem customScopes,
            List<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations, CurateOutcomeBuildItem curateOutcomeBuildItem) {
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
                        new BeanLookupSupplier(EventsMonitor.class), this.getClass(), curateOutcomeBuildItem));
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
                new BeanLookupSupplier(InvocationsMonitor.class), this.getClass(), curateOutcomeBuildItem));
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
        Collection<InterceptorInfo> removedInterceptors = validationContext.get(BuildExtension.Key.REMOVED_INTERCEPTORS);
        if (removedInterceptors != null) {
            for (InterceptorInfo interceptor : removedInterceptors) {
                beanInfos.addRemovedInterceptor(DevInterceptorInfo.from(interceptor, predicate));
            }
        }
        for (DecoratorInfo decorator : validationContext.get(BuildExtension.Key.DECORATORS)) {
            beanInfos.addDecorator(DevDecoratorInfo.from(decorator, predicate));
        }
        Collection<DecoratorInfo> removedDecorators = validationContext.get(BuildExtension.Key.REMOVED_DECORATORS);
        if (removedDecorators != null) {
            for (DecoratorInfo decorator : removedDecorators) {
                beanInfos.addRemovedDecorator(DevDecoratorInfo.from(decorator, predicate));
            }
        }

        // Build dependency graphs
        BeanResolver resolver = validationPhaseBuildItem.getBeanResolver();
        for (BeanInfo bean : validationContext.beans()) {
            beanInfos.addDependencyGraph(bean.getIdentifier(),
                    buildDependencyGraph(bean, validationContext, resolver, beanInfos));
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

    DependencyGraph buildDependencyGraph(BeanInfo bean, ValidationContext validationContext, BeanResolver resolver,
            DevBeanInfos devBeanInfos) {
        Set<DevBeanInfo> nodes = new HashSet<>();
        Collection<BeanInfo> beans = validationContext.get(BuildExtension.Key.BEANS);
        Set<Link> links = new HashSet<>();
        Map<BeanInfo, List<BeanInfo>> declaringToProducers = validationContext.beans().producers()
                .collect(Collectors.groupingBy(BeanInfo::getDeclaringBean));
        addNodesDependencies(bean, nodes, links, bean, devBeanInfos);
        addNodesDependents(bean, nodes, links, bean, beans, declaringToProducers, resolver, devBeanInfos);
        return new DependencyGraph(nodes, links);
    }

    void addNodesDependencies(BeanInfo root, Set<DevBeanInfo> nodes, Set<Link> links, BeanInfo bean,
            DevBeanInfos devBeanInfos) {
        if (nodes.add(devBeanInfos.getBean(bean.getIdentifier()))) {
            if (bean.isProducerField() || bean.isProducerMethod()) {
                links.add(Link.producer(bean.getIdentifier(), bean.getDeclaringBean().getIdentifier()));
                addNodesDependencies(root, nodes, links, bean.getDeclaringBean(), devBeanInfos);
            }
            for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
                BeanInfo resolved = injectionPoint.getResolvedBean();
                if (resolved != null && !resolved.equals(bean)) {
                    links.add(Link.dependency(root.equals(bean), bean.getIdentifier(), resolved.getIdentifier()));
                    // add transient dependencies
                    addNodesDependencies(root, nodes, links, injectionPoint.getResolvedBean(), devBeanInfos);
                }
            }
        }
    }

    void addNodesDependents(BeanInfo root, Set<DevBeanInfo> nodes, Set<Link> links, BeanInfo bean, Collection<BeanInfo> beans,
            Map<BeanInfo, List<BeanInfo>> declaringToProducers, BeanResolver resolver, DevBeanInfos devBeanInfos) {
        for (BeanInfo dependent : beans) {
            if (!bean.equals(dependent)) {
                for (InjectionPointInfo injectionPoint : dependent.getAllInjectionPoints()) {
                    Link link = null;
                    if (injectionPoint.isProgrammaticLookup()) {
                        if (resolver.matches(bean,
                                injectionPoint.getType().asParameterizedType().arguments().get(0),
                                injectionPoint.getRequiredQualifiers())) {
                            link = Link.lookup(dependent.getIdentifier(), bean.getIdentifier());
                        }
                    } else if (bean.equals(injectionPoint.getResolvedBean())) {
                        link = Link.dependent(root.equals(bean), dependent.getIdentifier(), bean.getIdentifier());
                    }
                    if (link != null) {
                        links.add(link);
                        if (nodes.add(devBeanInfos.getBean(dependent.getIdentifier()))) {
                            // add transient dependents
                            addNodesDependents(root, nodes, links, dependent, beans, declaringToProducers, resolver,
                                    devBeanInfos);
                        }
                    }
                }
            }
        }
        for (BeanInfo producer : declaringToProducers.getOrDefault(bean, Collections.emptyList())) {
            links.add(Link.producer(producer.getIdentifier(), bean.getIdentifier()));
            if (nodes.add(devBeanInfos.getBean(producer.getIdentifier()))) {
                // add transient dependents
                addNodesDependents(root, nodes, links, producer, beans, declaringToProducers, resolver,
                        devBeanInfos);
            }
        }
    }

}
