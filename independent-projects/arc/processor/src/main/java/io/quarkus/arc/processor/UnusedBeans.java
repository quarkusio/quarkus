package io.quarkus.arc.processor;

import static java.util.function.Predicate.not;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class UnusedBeans {

    private UnusedBeans() {
    }

    static Set<BeanInfo> findRemovableBeans(Collection<BeanInfo> beans, Collection<InjectionPointInfo> injectionPoints,
            Set<BeanInfo> declaresObserver, List<Predicate<BeanInfo>> allUnusedExclusions) {
        Set<BeanInfo> removableBeans = new HashSet<>();

        Set<BeanInfo> unusedProducers = new HashSet<>();
        Set<BeanInfo> unusedButDeclaresProducer = new HashSet<>();
        List<BeanInfo> producers = beans.stream().filter(b -> b.isProducerMethod() || b.isProducerField())
                .collect(Collectors.toList());
        List<InjectionPointInfo> instanceInjectionPoints = injectionPoints.stream()
                .filter(InjectionPointInfo::isProgrammaticLookup)
                .collect(Collectors.toList());
        // Collect all injected beans; skip delegate injection points and injection points that resolve to a built-in bean
        Set<BeanInfo> injected = injectionPoints.stream()
                .filter(not(InjectionPointInfo::isDelegate).and(InjectionPointInfo::hasResolvedBean))
                .map(InjectionPointInfo::getResolvedBean)
                .collect(Collectors.toSet());
        Set<BeanInfo> declaresProducer = producers.stream().map(BeanInfo::getDeclaringBean).collect(Collectors.toSet());

        // Beans - first pass to find unused beans that do not declare a producer
        test: for (BeanInfo bean : beans) {
            // Named beans can be used in templates and expressions
            if (bean.getName() != null) {
                continue test;
            }
            // Unremovable synthetic beans
            if (!bean.isRemovable()) {
                continue test;
            }
            // Custom exclusions
            for (Predicate<BeanInfo> exclusion : allUnusedExclusions) {
                if (exclusion.test(bean)) {
                    continue test;
                }
            }
            // Is injected
            if (injected.contains(bean)) {
                continue test;
            }
            // Declares an observer method
            if (declaresObserver.contains(bean)) {
                continue test;
            }
            // Instance<Foo>
            for (InjectionPointInfo injectionPoint : instanceInjectionPoints) {
                if (Beans.hasQualifiers(bean, injectionPoint.getRequiredQualifiers()) && Beans.matchesType(bean,
                        injectionPoint.getType().asParameterizedType().arguments().get(0))) {
                    continue test;
                }
            }
            // Declares a producer - see also second pass
            if (declaresProducer.contains(bean)) {
                unusedButDeclaresProducer.add(bean);
                continue test;
            }
            if (bean.isProducerField() || bean.isProducerMethod()) {
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
                }
            }
        }
        return removableBeans;
    }

}
