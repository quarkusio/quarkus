package org.jboss.resteasy.reactive.server.jaxrs;

import java.lang.reflect.Method;
import javax.ws.rs.container.ResourceInfo;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;

// TODO: this needs to go away...
public class DynamicFeatureResourceInfo implements ResourceInfo {

    private final Class<?> resourceClass;
    private final Method resourceMethod;

    public DynamicFeatureResourceInfo(ResourceClass resourceClass, ResourceMethod resourceMethod) {
        try {
            Class<?> clazz = Class.forName(resourceClass.getClassName(), false, Thread.currentThread().getContextClassLoader());
            Method[] methods = clazz.getMethods();
            Method method = null;
            for (Method m : methods) {
                if ((m.getName().equals(resourceMethod.getName()))
                        && (m.getParameterCount() == resourceMethod.getParameters().length)) {
                    if (m.getParameterCount() == 0) {
                        method = m;
                        break;
                    } else {
                        Class<?>[] parameterTypes = m.getParameterTypes();
                        boolean typesMatch = true;
                        for (int i = 0; i < parameterTypes.length; i++) {
                            if (!parameterTypes[i].getName().equals(resourceMethod.getParameters()[i].type)) {
                                typesMatch = false;
                                break;
                            }
                        }
                        if (typesMatch) {
                            method = m;
                        }
                    }
                }
                if (method != null) {
                    break;
                }
            }
            if (method == null) {
                throw new IllegalStateException();
            }
            this.resourceClass = clazz;
            this.resourceMethod = method;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public DynamicFeatureResourceInfo(Class<?> resourceClass, Method resourceMethod) {
        this.resourceClass = resourceClass;
        this.resourceMethod = resourceMethod;
    }

    @Override
    public Method getResourceMethod() {
        return resourceMethod;
    }

    @Override
    public Class<?> getResourceClass() {
        return resourceClass;
    }
}
