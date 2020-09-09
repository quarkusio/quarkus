package io.quarkus.rest.data.panache.deployment.methods;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;

public interface MethodImplementor {

    String APPLICATION_JSON = "application/json";

    String APPLICATION_HAL_JSON = "application/hal+json";

    void implement(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo);
}
