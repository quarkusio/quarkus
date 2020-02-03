package io.quarkus.deployment.builditem.nativeimage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.util.Comparators;

public final class ReflectiveMethodBuildItem extends MultiBuildItem implements Comparable<ReflectiveMethodBuildItem> {

    private static final Comparator<ReflectiveMethodBuildItem> COMPARATOR = Comparator
            .comparing((ReflectiveMethodBuildItem item) -> item.declaringClass)
            .thenComparing(item -> item.name)
            .thenComparing(item -> item.params, Comparators.forCollections());

    final String declaringClass;
    final String name;
    final List<String> params;

    public ReflectiveMethodBuildItem(MethodInfo methodInfo) {
        final int len = methodInfo.parameters().size();
        List<String> params = new ArrayList<>(len);
        for (int i = 0; i < len; ++i) {
            params.add(methodInfo.parameters().get(i).name().toString());
        }
        this.name = methodInfo.name();
        this.params = params;
        this.declaringClass = methodInfo.declaringClass().name().toString();
    }

    public ReflectiveMethodBuildItem(Method method) {
        final int len = method.getParameterTypes().length;
        final List<String> params = new ArrayList<>(len);
        for (int i = 0; i < len; ++i) {
            params.add(method.getParameterTypes()[i].getName());
        }
        this.params = params;
        this.name = method.getName();
        this.declaringClass = method.getDeclaringClass().getName();
    }

    public String getName() {
        return name;
    }

    public List<String> getParams() {
        return Collections.unmodifiableList(params);
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReflectiveMethodBuildItem that = (ReflectiveMethodBuildItem) o;
        return Objects.equals(declaringClass, that.declaringClass) &&
                Objects.equals(name, that.name) &&
                Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(declaringClass, name, params);
        return result;
    }

    @Override
    public int compareTo(ReflectiveMethodBuildItem other) {
        return COMPARATOR.compare(this, other);
    }
}
