package io.quarkus.jaxrs.client.reactive.deployment.beanparam;

import org.jboss.jandex.MethodInfo;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public class GetterExtractor extends ValueExtractor {

    private final MethodInfo getterMethod;

    public GetterExtractor(MethodInfo getterMethod) {
        this.getterMethod = getterMethod;
    }

    @Override
    ResultHandle extract(BytecodeCreator bytecodeCreator, ResultHandle containingObject) {
        return bytecodeCreator.invokeVirtualMethod(getterMethod, containingObject);
    }
}
