package io.quarkus.deployment.builditem.substrate;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem ReflectiveMethodBuildItem}
 *             instead.
 */
@Deprecated
public final class ReflectiveMethodBuildItem extends MultiBuildItem {

    final String declaringClass;
    final String name;
    final String[] params;
    private final MethodInfo methodInfo;
    private final Method method;

    public ReflectiveMethodBuildItem(MethodInfo methodInfo) {
        String[] params = new String[methodInfo.parameters().size()];
        for (int i = 0; i < params.length; ++i) {
            params[i] = methodInfo.parameters().get(i).name().toString();
        }
        this.name = methodInfo.name();
        this.params = params;
        this.declaringClass = methodInfo.declaringClass().name().toString();
        this.methodInfo = methodInfo;
        this.method = null;
    }

    public ReflectiveMethodBuildItem(Method method) {
        String[] params = new String[method.getParameterTypes().length];
        for (int i = 0; i < params.length; ++i) {
            params[i] = method.getParameterTypes()[i].getName();
        }
        this.params = params;
        this.name = method.getName();
        this.declaringClass = method.getDeclaringClass().getName();
        this.methodInfo = null;
        this.method = method;
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

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public Method getMethod() {
        return method;
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
