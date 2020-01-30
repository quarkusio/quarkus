package io.quarkus.qute.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class ImplicitValueResolverBuildItem extends MultiBuildItem {

    private final ClassInfo clazz;

    public ImplicitValueResolverBuildItem(ClassInfo clazz) {
        this.clazz = clazz;
    }

    public ClassInfo getClazz() {
        return clazz;
    }

}
