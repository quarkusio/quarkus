package io.quarkus.resteasy.reactive.links.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.links.runtime.GetterAccessor;

class GetterAccessorImplementor {

    /**
     * Implements a {@link GetterAccessor} that knows how to access a specific getter method of a specific type.
     */
    void implement(ClassOutput classOutput, GetterMetadata getterMetadata) {
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput).className(getterMetadata.getGetterAccessorName())
                .interfaces(GetterAccessor.class)
                .build();
        MethodCreator methodCreator = classCreator.getMethodCreator("get", Object.class, Object.class);
        ResultHandle value = methodCreator.invokeVirtualMethod(
                ofMethod(getterMetadata.getEntityType(), getterMetadata.getGetterName(), getterMetadata.getFieldType()),
                methodCreator.getMethodParam(0));
        methodCreator.returnValue(value);
        methodCreator.close();
        classCreator.close();
    }
}
