package io.quarkus.arc.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is used to specify one or more additional bean classes to be analyzed during bean discovery.
 * <p>
 * By default, the resulting beans may be removed if they are considered unused and {@link ArcConfig#removeUnusedBeans} is
 * enabled. You can change the default behavior by setting the {@link #removable} to {@code false} and via
 * {@link Builder#setUnremovable()}.
 * <p>
 * An additional bean may have the scope defaulted via {@link #defaultScope} and {@link Builder#setDefaultScope(DotName)}. The
 * default scope is only used if there is no scope declared on the bean class. The default scope should be used in cases where a
 * bean class source is not controlled by the extension and the scope annotation cannot be declared directly on the class.
 */
public final class AdditionalBeanBuildItem extends MultiBuildItem {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenient factory method to create an unremovable build item for a single bean class.
     * 
     * @param beanClass
     * @return a new build item
     */
    public static AdditionalBeanBuildItem unremovableOf(Class<?> beanClass) {
        return new AdditionalBeanBuildItem(Collections.singletonList(beanClass.getName()), false, null);
    }

    /**
     * Convenient factory method to create an unremovable build item for a single bean class.
     *
     * @param beanClass
     * @return a new build item
     */
    public static AdditionalBeanBuildItem unremovableOf(String beanClass) {
        return new AdditionalBeanBuildItem(Collections.singletonList(beanClass), false, null);
    }

    private final List<String> beanClasses;
    private final boolean removable;
    private final DotName defaultScope;

    public AdditionalBeanBuildItem(String... beanClasses) {
        this(Arrays.asList(beanClasses), true, null);
    }

    public AdditionalBeanBuildItem(Class<?>... beanClasses) {
        this(Arrays.stream(beanClasses).map(Class::getName).collect(Collectors.toList()), true, null);
    }

    AdditionalBeanBuildItem(List<String> beanClasses, boolean removable, DotName defaultScope) {
        this.beanClasses = beanClasses;
        this.removable = removable;
        this.defaultScope = defaultScope;
    }

    public List<String> getBeanClasses() {
        return Collections.unmodifiableList(beanClasses);
    }

    public boolean contains(String beanClass) {
        return beanClasses.contains(beanClass);
    }

    public boolean isRemovable() {
        return removable;
    }

    public DotName getDefaultScope() {
        return defaultScope;
    }

    public static class Builder {

        private final List<String> beanClasses;
        private boolean removable = true;
        private DotName defaultScope;

        public Builder() {
            this.beanClasses = new ArrayList<>();
        }

        public Builder addBeanClasses(Class<?>... beanClasses) {
            Arrays.stream(beanClasses).map(Class::getName).forEach(this.beanClasses::add);
            return this;
        }

        public Builder addBeanClasses(String... beanClasses) {
            Collections.addAll(this.beanClasses, beanClasses);
            return this;
        }

        public Builder addBeanClasses(Collection<String> beanClasses) {
            this.beanClasses.addAll(beanClasses);
            return this;
        }

        public Builder addBeanClass(String beanClass) {
            this.beanClasses.add(beanClass);
            return this;
        }

        public Builder addBeanClass(Class<?> beanClass) {
            this.beanClasses.add(beanClass.getName());
            return this;
        }

        public Builder setRemovable() {
            this.removable = true;
            return this;
        }

        public Builder setUnremovable() {
            this.removable = false;
            return this;
        }

        /**
         * The default scope is only used if there is no scope declared on the bean class.
         * <p>
         * The default scope should be used in cases where a bean class source is not controlled by the extension and the
         * scope annotation cannot be declared directly on the class.
         * 
         * @param defaultScope
         * @return self
         */
        public Builder setDefaultScope(DotName defaultScope) {
            this.defaultScope = defaultScope;
            return this;
        }

        public AdditionalBeanBuildItem build() {
            return new AdditionalBeanBuildItem(new ArrayList<>(beanClasses), removable, defaultScope);
        }

    }

}
