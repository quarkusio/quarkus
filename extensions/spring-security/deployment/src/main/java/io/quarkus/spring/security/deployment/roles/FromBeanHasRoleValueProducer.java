package io.quarkus.spring.security.deployment.roles;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.fieldDescOf;

import org.jboss.jandex.FieldInfo;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.MethodDesc;

public class FromBeanHasRoleValueProducer implements HasRoleValueProducer {

    private final String beanName;
    private final FieldInfo fieldInfo;

    public FromBeanHasRoleValueProducer(String beanName, FieldInfo fieldInfo) {
        this.beanName = beanName;
        this.fieldInfo = fieldInfo;
    }

    @Override
    public Expr apply(BlockCreator creator) {
        LocalVar arcContainer = creator.localVar("arcContainer", creator
                .invokeStatic(MethodDesc.of(Arc.class, "container", ArcContainer.class)));
        LocalVar instanceHandle = creator.localVar("instanceHandle", creator.invokeInterface(
                MethodDesc.of(ArcContainer.class, "instance", InstanceHandle.class, String.class),
                arcContainer, Const.of(beanName)));
        LocalVar bean = creator.localVar("bean", creator
                .invokeInterface(MethodDesc.of(InstanceHandle.class, "get", Object.class), instanceHandle));
        return creator.get(bean.field(fieldDescOf(fieldInfo)));
    }
}
