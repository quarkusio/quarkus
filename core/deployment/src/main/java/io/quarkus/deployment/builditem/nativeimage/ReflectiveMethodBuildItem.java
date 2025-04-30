package io.quarkus.deployment.builditem.nativeimage;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class ReflectiveMethodBuildItem extends MultiBuildItem {

    final String declaringClass;
    final String name;
    final String[] params;
    final boolean queryOnly;
    final String reason;

    public ReflectiveMethodBuildItem(MethodInfo methodInfo) {
        this(null, false, methodInfo);
    }

    public ReflectiveMethodBuildItem(String reason, MethodInfo methodInfo) {
        this(reason, false, methodInfo);
    }

    public ReflectiveMethodBuildItem(boolean queryOnly, MethodInfo methodInfo) {
        this(null, queryOnly, methodInfo);
    }

    public ReflectiveMethodBuildItem(String reason, boolean queryOnly, MethodInfo methodInfo) {
        this(reason, queryOnly, methodInfo.declaringClass().name().toString(), methodInfo.name(),
                methodInfo.parameterTypes().stream().map(p -> p.name().toString()).toArray(String[]::new));
    }

    public ReflectiveMethodBuildItem(Method method) {
        this(false, method);
    }

    public ReflectiveMethodBuildItem(boolean queryOnly, Method method) {
        this(null, queryOnly, method);
    }

    public ReflectiveMethodBuildItem(String reason, boolean queryOnly, Method method) {
        this(reason, queryOnly, method.getDeclaringClass().getName(), method.getName(),
                Arrays.stream(method.getParameterTypes()).map(Class::getName).toArray(String[]::new));
    }

    public ReflectiveMethodBuildItem(String declaringClass, String name,
            String... params) {
        this(null, false, declaringClass, name, params);
    }

    public ReflectiveMethodBuildItem(String reason, String declaringClass, String name,
            String... params) {
        this(reason, false, declaringClass, name, params);
    }

    public ReflectiveMethodBuildItem(boolean queryOnly, String declaringClass, String name,
            String... params) {
        this(null, queryOnly, declaringClass, name, params);
    }

    public ReflectiveMethodBuildItem(String reason, boolean queryOnly, String declaringClass, String name,
            String... params) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.params = params;
        this.queryOnly = queryOnly;
        this.reason = reason;
    }

    public ReflectiveMethodBuildItem(String reason, String declaringClass, String name,
            Class<?>... params) {
        this(reason, false, declaringClass, name, Arrays.stream(params).map(Class::getName).toArray(String[]::new));
    }

    public ReflectiveMethodBuildItem(String declaringClass, String name,
            Class<?>... params) {
        this(false, declaringClass, name, params);
    }

    public ReflectiveMethodBuildItem(boolean queryOnly, String declaringClass, String name,
            Class<?>... params) {
        this(null, queryOnly, declaringClass, name, Arrays.stream(params).map(Class::getName).toArray(String[]::new));
    }

    public String getName() {
        return name;
    }

    public String[] getParams() {
        return params;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public boolean isQueryOnly() {
        return queryOnly;
    }

    public String getReason() {
        return reason;
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
                Arrays.equals(params, that.params);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(declaringClass, name);
        result = 31 * result + Arrays.hashCode(params);
        return result;
    }
}
