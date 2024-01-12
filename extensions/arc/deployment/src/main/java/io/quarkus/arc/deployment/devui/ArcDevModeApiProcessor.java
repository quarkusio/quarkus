package io.quarkus.arc.deployment.devui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.DecoratorInfo;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.InterceptorInfo;
import io.quarkus.arc.processor.ObserverInfo;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.dev.console.DevConsoleManager;

public class ArcDevModeApiProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void collectBeanInfo(ArcConfig config, ValidationPhaseBuildItem validationPhaseBuildItem,
            CompletedApplicationClassPredicateBuildItem predicate,
            BuildProducer<ArcBeanInfoBuildItem> arcBeanInfoProducer) {
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
        Map<String, List<String>> beanDependenciesMap = new HashMap<>();
        if (config.devMode.generateDependencyGraphs) {
            BeanResolver resolver = validationPhaseBuildItem.getBeanResolver();
            Collection<BeanInfo> beans = validationContext.get(BuildExtension.Key.BEANS);
            Map<BeanInfo, List<InjectionPointInfo>> directDependents = new HashMap<>();
            List<InjectionPointInfo> allInjectionPoints = new ArrayList<>();
            Map<BeanInfo, List<BeanInfo>> declaringToProducers = validationContext.beans().producers()
                    .collect(Collectors.groupingBy(BeanInfo::getDeclaringBean));
            for (BeanInfo b : beans) {
                if (b.hasInjectionPoint()) {
                    for (InjectionPointInfo ip : b.getAllInjectionPoints()) {
                        if (ip.getTargetBean().isPresent()) {
                            allInjectionPoints.add(ip);
                        }
                    }
                }
            }
            for (BeanInfo bean : beans) {
                DependencyGraph dependencyGraph = buildDependencyGraph(bean, validationContext, resolver, beanInfos,
                        allInjectionPoints, declaringToProducers,
                        directDependents);
                beanInfos.addDependencyGraph(bean.getIdentifier(), dependencyGraph);
                // id -> [dep1Id, dep2Id]
                beanDependenciesMap.put(bean.getIdentifier(),
                        dependencyGraph.filterLinks(link -> link.type.equals("directDependency")).nodes.stream()
                                .map(DevBeanInfo::getId).filter(id -> !id.equals(bean.getIdentifier()))
                                .collect(Collectors.toList()));
            }
        }

        // Set the global that could be used at runtime when generating the json payload for /q/arc/beans
        DevConsoleManager.setGlobal(DevBeanInfos.BEAN_DEPENDENCIES, beanDependenciesMap);

        beanInfos.sort();

        arcBeanInfoProducer.produce(new ArcBeanInfoBuildItem(beanInfos));
    }

    DependencyGraph buildDependencyGraph(BeanInfo bean, ValidationContext validationContext, BeanResolver resolver,
            DevBeanInfos devBeanInfos, List<InjectionPointInfo> allInjectionPoints,
            Map<BeanInfo, List<BeanInfo>> declaringToProducers,
            Map<BeanInfo, List<InjectionPointInfo>> directDependents) {
        Set<DevBeanInfo> nodes = new HashSet<>();
        Set<Link> links = new HashSet<>();
        addNodesDependencies(0, bean, nodes, links, bean, devBeanInfos);
        addNodesDependents(0, bean, nodes, links, bean, allInjectionPoints, declaringToProducers, resolver, devBeanInfos,
                directDependents);
        return new DependencyGraph(nodes, links);
    }

    private void addNodesDependencies(int level, BeanInfo root, Set<DevBeanInfo> nodes, Set<Link> links, BeanInfo bean,
            DevBeanInfos devBeanInfos) {
        if (nodes.add(devBeanInfos.getBean(bean.getIdentifier()))) {
            if (bean.isProducerField() || bean.isProducerMethod()) {
                links.add(Link.producer(bean.getIdentifier(), bean.getDeclaringBean().getIdentifier(), level));
                addNodesDependencies(level + 1, root, nodes, links, bean.getDeclaringBean(), devBeanInfos);
            }
            for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
                BeanInfo resolved = injectionPoint.getResolvedBean();
                if (resolved != null && !resolved.equals(bean)) {
                    links.add(Link.dependency(bean.getIdentifier(), resolved.getIdentifier(), level));
                    // add transient dependencies
                    addNodesDependencies(level + 1, root, nodes, links, injectionPoint.getResolvedBean(), devBeanInfos);
                }
            }
        }
    }

    private void addNodesDependents(int level, BeanInfo root, Set<DevBeanInfo> nodes, Set<Link> links, BeanInfo bean,
            List<InjectionPointInfo> injectionPoints, Map<BeanInfo, List<BeanInfo>> declaringToProducers, BeanResolver resolver,
            DevBeanInfos devBeanInfos, Map<BeanInfo, List<InjectionPointInfo>> directDependents) {
        List<InjectionPointInfo> direct = directDependents.get(bean);
        if (direct == null) {
            direct = new ArrayList<>();
            for (InjectionPointInfo injectionPoint : injectionPoints) {
                BeanInfo dependent = injectionPoint.getTargetBean().get();
                if (dependent.equals(bean)) {
                    continue;
                }
                BeanInfo resolved = injectionPoint.getResolvedBean();
                if (resolved == null) {
                    if (injectionPoint.isProgrammaticLookup() && resolver.matches(bean,
                            injectionPoint.getType().asParameterizedType().arguments().get(0),
                            injectionPoint.getRequiredQualifiers())) {
                        direct.add(injectionPoint);
                    }
                } else if (bean.equals(resolved)) {
                    direct.add(injectionPoint);
                }
            }
            directDependents.put(bean, direct);
        }
        for (InjectionPointInfo ip : direct) {
            BeanInfo dependent = ip.getTargetBean().get();
            Link link;
            if (ip.getResolvedBean() == null) {
                link = Link.lookup(dependent.getIdentifier(), bean.getIdentifier(), level);
            } else {
                link = Link.dependent(dependent.getIdentifier(), bean.getIdentifier(), level);
            }
            links.add(link);
            if (nodes.add(devBeanInfos.getBean(dependent.getIdentifier()))) {
                // add transient dependents
                addNodesDependents(level + 1, root, nodes, links, dependent, injectionPoints, declaringToProducers, resolver,
                        devBeanInfos, directDependents);
            }
        }

        for (BeanInfo producer : declaringToProducers.getOrDefault(bean, Collections.emptyList())) {
            links.add(Link.producer(producer.getIdentifier(), bean.getIdentifier(), level));
            if (nodes.add(devBeanInfos.getBean(producer.getIdentifier()))) {
                // add transient dependents
                addNodesDependents(level + 1, root, nodes, links, producer, injectionPoints, declaringToProducers, resolver,
                        devBeanInfos, directDependents);
            }
        }
    }

}
