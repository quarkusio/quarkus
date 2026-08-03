package io.quarkus.deployment.builditem.nativeimage;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Registering methods for reflective access during the build
 */
public final class ReflectiveMethodBuildItem extends MultiBuildItem {

    private static final Logger log = Logger.getLogger(ReflectiveMethodBuildItem.class);

    final String declaringClass;
    final String name;
    final String[] params;
    final String reason;

    public ReflectiveMethodBuildItem(MethodInfo methodInfo) {
        this(null, methodInfo);
    }

    public ReflectiveMethodBuildItem(String reason, MethodInfo methodInfo) {
        this(reason, methodInfo.declaringClass().name().toString(), methodInfo.name(),
                methodInfo.parameterTypes().stream().map(p -> p.name().toString()).toArray(String[]::new));
    }

    public ReflectiveMethodBuildItem(Method method) {
        this(null, method);
    }

    public ReflectiveMethodBuildItem(String reason, Method method) {
        this(reason, method.getDeclaringClass().getName(), method.getName(),
                Arrays.stream(method.getParameterTypes()).map(Class::getName).toArray(String[]::new));
    }

    public ReflectiveMethodBuildItem(String declaringClass, String name, String... params) {
        this(null, declaringClass, name, params);
    }

    public ReflectiveMethodBuildItem(String reason, String declaringClass, String name, String... params) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.params = params;
        this.reason = reason;
    }

    /**
     * @deprecated "queryOnly" is a no-op for reachability-metadata.
     */
    @Deprecated(forRemoval = true, since = "3.36.0")
    public ReflectiveMethodBuildItem(String reason, boolean queryOnly, String declaringClass, String name, String... params) {
        this(reason, declaringClass, name, params);
        log.warn("Deprecated queryOnly flag of your ReflectiveMethodBuildItem is a no-op for reachability-metadata.json.");
    }

    /**
     * @deprecated "queryOnly" is a no-op for reachability-metadata.
     */
    @Deprecated(forRemoval = true, since = "3.36.0")
    public ReflectiveMethodBuildItem(String reason, boolean queryOnly, String declaringClass, String name) {
        this(reason, declaringClass, name, new String[0]);
        log.warn("Deprecated queryOnly flag of your ReflectiveMethodBuildItem is a no-op for reachability-metadata.json.");
    }

    public ReflectiveMethodBuildItem(String reason, String declaringClass, String name, Class<?>... params) {
        this(reason, declaringClass, name, Arrays.stream(params).map(Class::getName).toArray(String[]::new));
    }

    public ReflectiveMethodBuildItem(String declaringClass, String name, Class<?>... params) {
        this(null, declaringClass, name, params);
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
