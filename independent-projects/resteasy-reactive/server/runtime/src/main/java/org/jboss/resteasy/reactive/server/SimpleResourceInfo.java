package org.jboss.resteasy.reactive.server;

import jakarta.ws.rs.container.ResourceInfo;
import java.lang.reflect.Method;

/**
 * Type that can be injected into places where ResourceInfo can.
 * The idea is that this can be used when a piece of code does not need access to the entire resource method
 * (which entails a reflective lookup call), where the resource class, method name and parameter types will suffice
 */
public interface SimpleResourceInfo {

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

    class NullValues implements SimpleResourceInfo, ResourceInfo {

        public static final NullValues INSTANCE = new NullValues();

        private NullValues() {
        }

        @Override
        public Method getResourceMethod() {
            return null;
        }

        @Override
        public Class<?> getResourceClass() {
            return null;
        }

        @Override
        public String getMethodName() {
            return null;
        }

        @Override
        public Class<?>[] parameterTypes() {
            return new Class[0];
        }
    }
}
