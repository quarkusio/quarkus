package io.quarkus.arc.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * This build item instructs ArC to produce a {@link ReflectiveClassBuildItem} for a client proxy and intercepted
 * subclass generated for the given bean class.
 */
public final class ReflectiveBeanClassBuildItem extends MultiBuildItem {

    private final DotName className;

    public ReflectiveBeanClassBuildItem(ClassInfo classInfo) {
        this(classInfo.name());
    }

    public ReflectiveBeanClassBuildItem(String className) {
        this.className = DotName.createSimple(className);
    }

    public ReflectiveBeanClassBuildItem(DotName className) {
        this.className = className;
    }

    public DotName getClassName() {
        return className;
    }

}
