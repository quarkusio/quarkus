package io.quarkus.resteasy.jsonb.deployment.serializers;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.Type;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.deployment.DotNames;

public class IntegerTypeSerializerGenerator extends AbstractNumberTypeSerializerGenerator {

    @Override
    public Supported supports(Type type, TypeSerializerGeneratorRegistry registry) {
        return DotNames.INTEGER.equals(type.name()) ? Supported.FULLY : Supported.UNSUPPORTED;
    }

    @Override
    public void generateUnformatted(GenerateContext context) {
        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();

        ResultHandle intValueHandle = bytecodeCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Integer.class, "intValue", int.class),
                context.getCurrentItem());

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "write", JsonGenerator.class, int.class),
                context.getJsonGenerator(),
                intValueHandle);
    }

}
