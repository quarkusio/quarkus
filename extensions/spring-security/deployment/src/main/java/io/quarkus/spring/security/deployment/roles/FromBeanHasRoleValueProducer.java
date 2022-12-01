package io.quarkus.spring.security.deployment.roles;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import org.jboss.jandex.FieldInfo;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class FromBeanHasRoleValueProducer implements HasRoleValueProducer {

    private final String beanName;
    private final FieldInfo fieldInfo;

    public FromBeanHasRoleValueProducer(String beanName, FieldInfo fieldInfo) {
        this.beanName = beanName;
        this.fieldInfo = fieldInfo;
    }

    @Override
    public ResultHandle apply(BytecodeCreator creator) {
        ResultHandle arcContainer = creator
                .invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = creator.invokeInterfaceMethod(
                ofMethod(ArcContainer.class, "instance", InstanceHandle.class, String.class),
                arcContainer, creator.load(beanName));
        ResultHandle bean = creator
                .invokeInterfaceMethod(ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);
        return creator.readInstanceField(FieldDescriptor.of(fieldInfo), bean);
    }
}
