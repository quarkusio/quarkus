package org.jboss.resteasy.reactive.client.processor.beanparam;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.jboss.resteasy.reactive.client.impl.ReflectionUtil;

public class FieldExtractor extends ValueExtractor {

    final String companionClass;
    private final String className;
    private final String fieldName;

    public FieldExtractor(String companionClass, String fieldName, String className) {
        this.companionClass = companionClass;
        this.fieldName = fieldName;
        this.className = className;
    }

    @Override
    ResultHandle extract(BytecodeCreator bytecodeCreator, ResultHandle containingObject) {
        MethodDescriptor readField = MethodDescriptor.ofMethod(ReflectionUtil.class, "readField", Object.class, Object.class,
                Class.class, String.class);
        return bytecodeCreator.invokeStaticMethod(readField, containingObject, bytecodeCreator.loadClass(className),
                bytecodeCreator.load(fieldName));
    }
}
