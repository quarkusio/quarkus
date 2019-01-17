package org.jboss.shamrock.arc.deployment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.protean.arc.processor.BeanInfo;

/**
 * This build item is used to exclude beans that would be normally removed if the config property {@link ArcConfig#removeUnusedBeans} is set to true.
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
