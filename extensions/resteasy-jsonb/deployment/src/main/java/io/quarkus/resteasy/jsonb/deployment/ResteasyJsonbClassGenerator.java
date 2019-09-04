package io.quarkus.resteasy.jsonb.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

import javax.json.bind.Jsonb;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

class ResteasyJsonbClassGenerator {

    static final String QUARKUS_CONTEXT_RESOLVER = "io.quarkus.jsonb.QuarkusJsonbContextResolver";

    void generateJsonbContextResolver(ClassOutput classOutput) {
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(QUARKUS_CONTEXT_RESOLVER)
                .interfaces(ContextResolver.class)
                .signature("Ljava/lang/Object;Ljavax/ws/rs/ext/ContextResolver<Ljavax/json/bind/Jsonb;>;")
                .build()) {

            cc.addAnnotation(Provider.class);

            FieldDescriptor instance = cc.getFieldCreator("INSTANCE", Jsonb.class)
                    .setModifiers(Modifier.STATIC | Modifier.PRIVATE)
                    .getFieldDescriptor();

            try (MethodCreator getContext = cc.getMethodCreator("getContext", Jsonb.class, Class.class)) {
                BranchResult branchResult = getContext.ifNull(getContext.readStaticField(instance));

                BytecodeCreator instanceNotNull = branchResult.falseBranch();
                instanceNotNull.returnValue(instanceNotNull.readStaticField(instance));

                BytecodeCreator instanceNull = branchResult.trueBranch();

                ResultHandle arcContainer = instanceNull
                        .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));

                ResultHandle instanceHandle = instanceNull.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                                Annotation[].class),
                        arcContainer, instanceNull.loadClass(Jsonb.class), instanceNull.loadNull());
                ResultHandle get = instanceNull.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                        instanceHandle);

                ResultHandle jsonb = instanceNull.checkCast(get, Jsonb.class);

                instanceNull.writeStaticField(instance, jsonb);
                instanceNull.returnValue(jsonb);
            }

            try (MethodCreator bridgeGetContext = cc.getMethodCreator("getContext", Object.class, Class.class)) {
                MethodDescriptor getContext = MethodDescriptor.ofMethod(QUARKUS_CONTEXT_RESOLVER, "getContext",
                        "javax.json.bind.Jsonb",
                        "java.lang.Class");
                ResultHandle result = bridgeGetContext.invokeVirtualMethod(getContext, bridgeGetContext.getThis(),
                        bridgeGetContext.getMethodParam(0));
                bridgeGetContext.returnValue(result);
                bridgeGetContext.returnValue(bridgeGetContext.readStaticField(instance));
            }
        }
    }
}
