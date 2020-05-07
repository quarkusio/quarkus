package io.quarkus.panache.rest.common.deployment.methods;

import javax.ws.rs.core.Response;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.panache.rest.common.deployment.MethodImplementor;

public final class NotExposedMethodImplementor implements MethodImplementor {

    private final String name;

    private final String[] parameterTypes;

    public NotExposedMethodImplementor(String name, String... parameterTypes) {
        this.name = name;
        this.parameterTypes = parameterTypes;
    }

    @Override
    public void implement(ClassCreator classCreator) {
        MethodCreator methodCreator = classCreator.getMethodCreator(name, Response.class.getName(), parameterTypes);
        methodCreator.throwException(RuntimeException.class, String.format("'%s' method is not exposed", name));
        methodCreator.close();
    }
}
