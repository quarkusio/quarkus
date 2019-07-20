package io.quarkus.resteasy.jsonb.deployment.serializers;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.Type;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.deployment.DotNames;

public class BooleanTypeSerializerGenerator extends AbstractTypeSerializerGenerator {

    @Override
    public Supported supports(Type type, TypeSerializerGeneratorRegistry registry) {
        return DotNames.BOOLEAN.equals(type.name()) ? Supported.FULLY : Supported.UNSUPPORTED;
    }

    @Override
    protected void generateNotNull(GenerateContext context) {
        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();

        ResultHandle booleanValueHandle = bytecodeCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Boolean.class, "booleanValue", boolean.class),
                context.getCurrentItem());

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "write", JsonGenerator.class, boolean.class),
                context.getJsonGenerator(),
                booleanValueHandle);
    }
}
