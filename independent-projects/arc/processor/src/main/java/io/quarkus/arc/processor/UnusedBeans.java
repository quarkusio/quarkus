package io.quarkus.arc.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.InjectionPointInfo.TypeAndQualifiers;

final class UnusedBeans {

    private static final Logger LOG = Logger.getLogger(UnusedBeans.class);

    private UnusedBeans() {
    }

    static Set<BeanInfo> findRemovableBeans(BeanResolver beanResolver, Collection<BeanInfo> beans,
            Iterable<InjectionPointInfo> injectionPoints, Set<BeanInfo> declaresObserver,
            Set<BeanInfo> invokerLookups, List<Predicate<BeanInfo>> allUnusedExclusions) {
        Set<BeanInfo> removableBeans = new HashSet<>();

        Set<BeanInfo> unusedProducers = new HashSet<>();
        Set<BeanInfo> unusedButDeclaresProducer = new HashSet<>();
        List<BeanInfo> producers = beans.stream().filter(BeanInfo::isProducer)
                .collect(Collectors.toList());
        // Collect all:
        // - injected beans; skip delegate injection points and injection points that resolve to a built-in bean
        // - Instance<> injection points
        // - @All List<> injection points
        Set<BeanInfo> injected = new HashSet<>();
        List<InjectionPointInfo> instanceInjectionPoints = new ArrayList<>();
        List<TypeAndQualifiers> listAllInjectionPoints = new ArrayList<>();

        for (InjectionPointInfo injectionPoint : injectionPoints) {
            if (injectionPoint.isProgrammaticLookup()) {
                instanceInjectionPoints.add(injectionPoint);
            } else if (!injectionPoint.isDelegate()) {
                BeanInfo resolved = injectionPoint.getResolvedBean();
                if (resolved != null) {
                    injected.add(resolved);
                } else {
                    BuiltinBean builtin = BuiltinBean.resolve(injectionPoint);
                    if (builtin == BuiltinBean.LIST) {
                        Type requiredType = injectionPoint.getType().asParameterizedType().arguments().get(0);
                        if (requiredType.name().equals(DotNames.INSTANCE_HANDLE)) {
                            requiredType = requiredType.asParameterizedType().arguments().get(0);
                        }
                        Set<AnnotationInstance> qualifiers = new HashSet<>(injectionPoint.getRequiredQualifiers());
                        for (Iterator<AnnotationInstance> it = qualifiers.iterator(); it.hasNext();) {
                            AnnotationInstance qualifier = it.next();
                            if (qualifier.name().equals(DotNames.ALL)) {
                                it.remove();
                            }
                        }
                        listAllInjectionPoints.add(new TypeAndQualifiers(requiredType, qualifiers));
                    }
                }
            }
        }
        Set<BeanInfo> declaresProducer = producers.stream().map(BeanInfo::getDeclaringBean).collect(Collectors.toSet());

        // Beans - first pass to find unused beans that do not declare a producer
        test: for (BeanInfo bean : beans) {
            // Named beans can be used in templates and expressions
            if (bean.getName() != null) {
                LOG.debugf("Unremovable - named: %s", bean);
                continue test;
            }
            // Unremovable synthetic beans
            if (!bean.isRemovable()) {
                LOG.debugf("Unremovable - unremovable synthetic: %s", bean);
                continue test;
            }
            // Custom exclusions
            for (Predicate<BeanInfo> exclusion : allUnusedExclusions) {
                if (exclusion.test(bean)) {
                    LOG.debugf("Unremovable - excluded by %s: %s", exclusion.toString(), bean);
                    continue test;
                }
            }
            // Is injected
            if (injected.contains(bean)) {
                LOG.debugf("Unremovable - injected: %s", bean);
                continue test;
            }
            // Declares an observer method
            if (declaresObserver.contains(bean)) {
                LOG.debugf("Unremovable - declares observer: %s", bean);
                continue test;
            }
            // Result of an invoker lookup
            if (invokerLookups.contains(bean)) {
                LOG.debugf("Unremovable - invoker lookup result: %s", bean);
                continue test;
            }
            // Instance<Foo>
            for (InjectionPointInfo injectionPoint : instanceInjectionPoints) {
                if (Beans.hasQualifiers(bean, injectionPoint.getRequiredQualifiers())
                        && beanResolver.matchesType(bean,
                                injectionPoint.getType().asParameterizedType().arguments().get(0))) {
                    LOG.debugf("Unremovable - programmatic lookup: %s", bean);
                    continue test;
                }
            }
            // @All List<Foo>
            for (TypeAndQualifiers tq : listAllInjectionPoints) {
                if (Beans.hasQualifiers(bean, tq.qualifiers)
                        && beanResolver.matchesType(bean, tq.type)) {
                    LOG.debugf("Unremovable - @All List: %s", bean);
                    continue test;
                }
            }
            // Declares a producer - see also second pass
            if (declaresProducer.contains(bean)) {
                unusedButDeclaresProducer.add(bean);
                continue test;
            }
            if (bean.isProducer()) {
                // This bean is very likely an unused producer
                unusedProducers.add(bean);
            }
            removableBeans.add(bean);
        }
        if (!unusedProducers.isEmpty()) {
            // Beans - second pass to find beans which themselves are unused and declare only unused producers
            Map<BeanInfo, List<BeanInfo>> declaringMap = producers.stream()
                    .collect(Collectors.groupingBy(BeanInfo::getDeclaringBean));
            for (Entry<BeanInfo, List<BeanInfo>> entry : declaringMap.entrySet()) {
                BeanInfo declaringBean = entry.getKey();
                if (unusedButDeclaresProducer.contains(declaringBean) && unusedProducers.containsAll(entry.getValue())) {
                    // All producers declared by this bean are unused
                    removableBeans.add(declaringBean);
                } else {
                    LOG.debugf("Unremovable - declares producer: %s", declaringBean);
                }
            }
        }
        return removableBeans;
    }

}
