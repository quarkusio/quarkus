package io.quarkus.deployment.builditem.nativeimage;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * JNI access registration fine-grained to single methods
 * for a given class.
 */
public final class JniRuntimeAccessMethodBuildItem extends MultiBuildItem {

    final String declaringClass;
    final String name;
    final String[] params;

    public JniRuntimeAccessMethodBuildItem(MethodInfo methodInfo) {
        String[] params = new String[methodInfo.parametersCount()];
        for (int i = 0; i < params.length; ++i) {
            params[i] = methodInfo.parameterType(i).name().toString();
        }
        this.name = methodInfo.name();
        this.params = params;
        this.declaringClass = methodInfo.declaringClass().name().toString();
    }

    public JniRuntimeAccessMethodBuildItem(Method method) {
        this.params = new String[method.getParameterCount()];
        if (method.getParameterCount() > 0) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < params.length; ++i) {
                params[i] = parameterTypes[i].getName();
            }
        }
        this.name = method.getName();
        this.declaringClass = method.getDeclaringClass().getName();
    }

    public JniRuntimeAccessMethodBuildItem(String declaringClass, String name, String... params) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.params = params;
    }

    public JniRuntimeAccessMethodBuildItem(String declaringClass, String name, Class<?>... params) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.params = new String[params.length];
        for (int i = 0; i < params.length; ++i) {
            this.params[i] = params[i].getName();
        }
    }

    public JniRuntimeAccessMethodBuildItem(String declaringClass, String name) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.params = new String[0];
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JniRuntimeAccessMethodBuildItem that = (JniRuntimeAccessMethodBuildItem) o;
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
