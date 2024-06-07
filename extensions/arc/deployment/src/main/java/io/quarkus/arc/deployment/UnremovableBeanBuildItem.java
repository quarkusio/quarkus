package io.quarkus.arc.deployment;

import static io.quarkus.arc.processor.Annotations.getAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.annotation.CheckReturnValue;

/**
 * This build item is used to exclude beans that would be normally removed if the config property
 * {@link ArcConfig#removeUnusedBeans} is set to true.
 * <p>
 * Consider using one of the convenient static factory methods such as {@link #beanTypes(Class...)}:
 *
 * <pre>
 * &#64;BuildStep
 * UnremovableBeanBuildItem unremovable() {
 *     // Any bean that has MyService in its set of bean types is considered unremovable
 *     return UnremovableBeanBuildItem.beanTypes(MyService.class);
 * }
 * </pre>
 *
 * Alternatively, you could make use of the pre-built predicate classes such as {@link BeanClassNameExclusion}:
 *
 * <pre>
 * &#64;BuildStep
 * UnremovableBeanBuildItem unremovable() {
 *     // A bean whose bean class FQCN is equal to org.acme.MyService is considered unremovable
 *     return new UnremovableBeanBuildItem(new BeanClassNameExclusion("org.acme.MyService"));
 * }
 * </pre>
 */
public final class UnremovableBeanBuildItem extends MultiBuildItem {

    private final Predicate<BeanInfo> predicate;
    private final Set<String> classNames;

    public UnremovableBeanBuildItem(Predicate<BeanInfo> predicate) {
        this.predicate = predicate;
        this.classNames = Collections.emptySet();
    }

    public UnremovableBeanBuildItem(BeanClassNameExclusion predicate) {
        this.predicate = predicate;
        this.classNames = Collections.singleton(predicate.className);
    }

    public UnremovableBeanBuildItem(BeanClassNamesExclusion predicate) {
        this.predicate = predicate;
        this.classNames = predicate.classNames;
    }

    public UnremovableBeanBuildItem(BeanTypeExclusion predicate) {
        this.predicate = predicate;
        this.classNames = Collections.singleton(predicate.dotName.toString());
    }

    public UnremovableBeanBuildItem(BeanTypesExclusion predicate) {
        this.predicate = predicate;
        this.classNames = predicate.dotNames.stream().map(DotName::toString).collect(Collectors.toSet());
    }

    public Predicate<BeanInfo> getPredicate() {
        return predicate;
    }

    public Set<String> getClassNames() {
        return classNames;
    }

    /**
     * Match beans whose bean class matches any of the specified class names.
     *
     * @param classNames
     * @return a new build item
     */
    @CheckReturnValue
    public static UnremovableBeanBuildItem beanClassNames(String... classNames) {
        Set<String> names = new HashSet<>();
        Collections.addAll(names, classNames);
        return new UnremovableBeanBuildItem(new BeanClassNamesExclusion(names));
    }

    /**
     * Match beans whose bean class matches any of the specified class names.
     *
     * @param classNames
     * @return a new build item
     */
    @CheckReturnValue
    public static UnremovableBeanBuildItem beanClassNames(Set<String> classNames) {
        return new UnremovableBeanBuildItem(new BeanClassNamesExclusion(classNames));
    }

    /**
     * Match beans which have any of the specified type names in its set of bean types.
     *
     * @param typeNames
     * @return a new build item
     */
    @CheckReturnValue
    public static UnremovableBeanBuildItem beanTypes(DotName... typeNames) {
        Set<DotName> names = new HashSet<>();
        Collections.addAll(names, typeNames);
        return new UnremovableBeanBuildItem(new BeanTypesExclusion(names));
    }

    /**
     * Match beans which have any of the specified type names in its set of bean types.
     *
     * @param typeNames
     * @return a new build item
     */
    @CheckReturnValue
    public static UnremovableBeanBuildItem beanTypes(Class<?>... types) {
        return new UnremovableBeanBuildItem(new BeanTypesExclusion(
                Arrays.stream(types).map(Class::getName).map(DotName::createSimple).collect(Collectors.toSet())));
    }

    /**
     * Match beans which have any of the specified type names in its set of bean types.
     *
     * @param typeNames
     * @return a new build item
     */
    @CheckReturnValue
    public static UnremovableBeanBuildItem beanTypes(Set<DotName> typeNames) {
        return new UnremovableBeanBuildItem(new BeanTypesExclusion(typeNames));
    }

    /**
     * Match class beans whose target class contains the specified annotation.
     * <p>
     * The annotations can be declared on the class, and every nested element of the class (fields, types, methods, etc).
     *
     * @param annotationName
     * @return a new build item
     */
    @CheckReturnValue
    public static UnremovableBeanBuildItem beanClassAnnotation(DotName annotationName) {
        return new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(annotationName));
    }

    /**
     * Match class beans whose target class contains an annotation whose name starts with the specified value.
     * <p>
     * The annotations can be declared on the class, and every nested element of the class (fields, types, methods, etc).
     *
     * @param annotationName
     * @return a new build item
     */
    @CheckReturnValue
    public static UnremovableBeanBuildItem beanClassAnnotation(String nameStartsWith) {
        return new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(nameStartsWith));
    }

    /**
     * Match beans whose target (class, method or field) is annotated with the specified annotation.
     *
     * @param annotationName
     * @return a new build item
     */
    @CheckReturnValue
    public static UnremovableBeanBuildItem targetWithAnnotation(DotName annotationName) {
        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo bean) {
                if (bean.isClassBean()) {
                    return Annotations.contains(bean.getTarget().get().asClass().declaredAnnotations(), annotationName);
                } else if (bean.isProducerMethod()) {
                    return !getAnnotations(Kind.METHOD, annotationName, bean.getTarget().get().asMethod().annotations())
                            .isEmpty();
                } else if (bean.isProducerField()) {
                    return bean.getTarget().get().asField().hasAnnotation(annotationName);
                }
                // No target - synthetic bean
                return false;
            }
        });
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

        @Override
        public String toString() {
            return "BeanClassNameExclusion [className=" + className + "]";
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

        @Override
        public String toString() {
            return "BeanClassNamesExclusion [classNames=" + classNames + "]";
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

        @Override
        public String toString() {
            return "BeanTypeExclusion [dotName=" + dotName + "]";
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

        @Override
        public String toString() {
            return "BeanTypesExclusion [dotNames=" + dotNames + "]";
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
                Map<DotName, List<AnnotationInstance>> annotations = bean.getTarget().get().asClass().annotationsMap();
                if (name != null) {
                    return annotations.containsKey(name);
                } else {
                    return annotations.keySet().stream().anyMatch(a -> a.toString().startsWith(nameStartsWith));
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "BeanClassAnnotationExclusion [nameStartsWith=" + nameStartsWith + ", name=" + name + "]";
        }

    }

}
