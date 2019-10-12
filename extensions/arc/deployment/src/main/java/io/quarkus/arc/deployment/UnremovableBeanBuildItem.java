package io.quarkus.arc.deployment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is used to exclude beans that would be normally removed if the config property
 * {@link ArcConfig#removeUnusedBeans} is set to true.
 */
public final class UnremovableBeanBuildItem extends MultiBuildItem {

    private final Predicate<BeanInfo> predicate;

    public UnremovableBeanBuildItem(Predicate<BeanInfo> predicate) {
        this.predicate = predicate;
    }

    public Predicate<BeanInfo> getPredicate() {
        return predicate;
    }

    public static class BeanClassNameExclusion implements Predicate<BeanInfo> {

        private final String className;

        public BeanClassNameExclusion(String className) {
            this.className = Objects.requireNonNull(className);
        }

        @Override
        public boolean test(BeanInfo bean) {
            return bean.getBeanClass().toString().equals(className);
        }

    }

    public static class BeanClassNamesExclusion implements Predicate<BeanInfo> {

        private final Set<String> classNames;

        public BeanClassNamesExclusion(Set<String> classNames) {
            this.classNames = Objects.requireNonNull(classNames);
        }

        @Override
        public boolean test(BeanInfo bean) {
            return classNames.contains(bean.getBeanClass().toString());
        }

    }

    public static class BeanTypeExclusion implements Predicate<BeanInfo> {

        private final DotName dotName;

        public BeanTypeExclusion(DotName dotName) {
            this.dotName = Objects.requireNonNull(dotName);
        }

        @Override
        public boolean test(BeanInfo bean) {
            return bean.getTypes().stream().anyMatch(t -> dotName.equals(t.name()));
        }

    }

    public static class BeanTypesExclusion implements Predicate<BeanInfo> {

        private final Set<DotName> dotNames;

        public BeanTypesExclusion(Set<DotName> dotNames) {
            this.dotNames = Objects.requireNonNull(dotNames);
        }

        @Override
        public boolean test(BeanInfo bean) {
            return bean.getTypes().stream().anyMatch(t -> dotNames.contains(t.name()));
        }

    }

    public static class BeanClassAnnotationExclusion implements Predicate<BeanInfo> {

        private final String nameStartsWith;

        private final DotName name;

        public BeanClassAnnotationExclusion(String nameStartsWith) {
            this.nameStartsWith = Objects.requireNonNull(nameStartsWith);
            this.name = null;
        }

        public BeanClassAnnotationExclusion(DotName name) {
            this.nameStartsWith = null;
            this.name = name;
        }

        @Override
        public boolean test(BeanInfo bean) {
            if (bean.isClassBean()) {
                Map<DotName, List<AnnotationInstance>> annotations = bean.getTarget().get().asClass().annotations();
                if (name != null) {
                    return annotations.containsKey(name);
                } else {
                    return annotations.keySet().stream().anyMatch(a -> a.toString().startsWith(nameStartsWith));
                }
            }
            return false;
        }

    }

}
