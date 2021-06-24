package org.jboss.resteasy.reactive.client.processor.beanparam;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import org.jboss.jandex.MethodInfo;

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
