package io.quarkus.deployment.builditem.nativeimage;

import java.lang.reflect.Field;
import java.util.Comparator;

import org.jboss.jandex.FieldInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class ReflectiveFieldBuildItem extends MultiBuildItem implements Comparable<ReflectiveFieldBuildItem> {

    private static final Comparator<ReflectiveFieldBuildItem> COMPARATOR = Comparator
            .comparing((ReflectiveFieldBuildItem item) -> item.declaringClass)
            .thenComparing(item -> item.name);

    final String declaringClass;
    final String name;

    public ReflectiveFieldBuildItem(FieldInfo field) {
        this.name = field.name();
        this.declaringClass = field.declaringClass().name().toString();
    }

    public ReflectiveFieldBuildItem(Field field) {
        this.name = field.getName();
        this.declaringClass = field.getDeclaringClass().getName();
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(ReflectiveFieldBuildItem other) {
        return COMPARATOR.compare(this, other);
    }
}
