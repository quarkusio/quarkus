package io.quarkus.resteasy.reactive.server.runtime.security;

import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;

import io.quarkus.security.spi.runtime.MethodDescription;

/**
 * @param invokedMethodDesc description of actually invoked method (method on which CDI interceptors are applied)
 * @param fallbackMethodDesc description that we used in the past; not null when different to {@code invokedMethodDesc}
 */
record ResourceMethodDescription(MethodDescription invokedMethodDesc, MethodDescription fallbackMethodDesc) {

    static ResourceMethodDescription of(ServerResourceMethod method) {
        return new ResourceMethodDescription(
                createMethodDescription(method, method.getActualDeclaringClassName()),
                createMethodDescription(method, method.getClassDeclMethodThatHasJaxRsEndpointDefiningAnn()));
    }

    private static MethodDescription createMethodDescription(ServerResourceMethod method, String clazz) {
        if (clazz == null) {
            return null;
        }
        String[] paramTypes = new String[method.getParameters().length];
        for (int i = 0; i < method.getParameters().length; i++) {
            paramTypes[i] = method.getParameters()[i].declaredUnresolvedType;
        }

        return new MethodDescription(clazz, method.getName(), paramTypes);
    }

}
