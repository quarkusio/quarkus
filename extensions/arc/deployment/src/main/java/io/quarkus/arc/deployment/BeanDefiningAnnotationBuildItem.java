package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is used to specify additional bean defining annotations. See also
 * <a href="http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#bean_defining_annotations">2.5.1. Bean defining annotations</a>.
 * <p>
 * By default, the resulting beans must not be removed even if they are considered unused and
 * {@link ArcConfig#removeUnusedBeans} is enabled.
 */
public final class BeanDefiningAnnotationBuildItem extends MultiBuildItem {

    private final DotName name;
    private final DotName defaultScope;
    private final boolean removable;

    public BeanDefiningAnnotationBuildItem(DotName name) {
        this(name, null);
    }

    public BeanDefiningAnnotationBuildItem(DotName name, DotName defaultScope) {
        this(name, defaultScope, false);
    }

    public BeanDefiningAnnotationBuildItem(DotName name, DotName defaultScope, boolean removable) {
        this.name = name;
        this.defaultScope = defaultScope;
        this.removable = removable;
    }

    public DotName getName() {
        return name;
    }

    public DotName getDefaultScope() {
        return defaultScope;
    }

    /**
     * 
     * @return true if the resulting beans should be removed if they're considered unused as described in
     *         {@link ArcConfig#removeUnusedBeans}
     */
    public boolean isRemovable() {
        return removable;
    }

}
