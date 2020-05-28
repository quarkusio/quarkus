package io.quarkus.rest.data.panache.deployment.methods.internal;

import java.lang.reflect.Modifier;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.rest.data.panache.deployment.PrivateMethods;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.methods.MethodImplementor;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;

public class IsPagedMethodImplementor implements MethodImplementor {

    private final boolean isPaged;

    public IsPagedMethodImplementor(boolean isPaged) {
        this.isPaged = isPaged;
    }

    @Override
    public void implement(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodCreator methodCreator = classCreator
                .getMethodCreator(PrivateMethods.IS_PAGED.getName(), PrivateMethods.IS_PAGED.getType())
                .setModifiers(Modifier.PRIVATE);
        methodCreator.returnValue(methodCreator.load(isPaged));
        methodCreator.close();
    }
}
