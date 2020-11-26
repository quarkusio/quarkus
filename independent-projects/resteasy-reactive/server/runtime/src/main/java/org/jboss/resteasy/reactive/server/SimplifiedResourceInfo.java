package org.jboss.resteasy.reactive.server;

/**
 * Type that can be injected into places where ResourceInfo can.
 * The idea is that this can be used when a piece of code does not need access to the entire resource method
 * (which entails a reflective lookup call), where the resource class, method name and parameter types will suffice
 */
public interface SimplifiedResourceInfo {

    /**
     * Get the resource class that is the target of a request,
     */
    Class<?> getResourceClass();

    /**
     * Get the name of the resource method that is the target of a request
     */
    String getMethodName();

    /**
     * Get the parameter types of the resource method that is the target of a request
     */
    Class<?>[] parameterTypes();
}
