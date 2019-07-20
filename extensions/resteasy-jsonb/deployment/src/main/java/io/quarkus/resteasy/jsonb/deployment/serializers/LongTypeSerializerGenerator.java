package io.quarkus.resteasy.jsonb.deployment.serializers;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.Type;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.deployment.DotNames;

public class LongTypeSerializerGenerator extends AbstractNumberTypeSerializerGenerator {

    @Override
    public Supported supports(Type type, TypeSerializerGeneratorRegistry registry) {
        return DotNames.LONG.equals(type.name()) ? Supported.FULLY : Supported.UNSUPPORTED;
    }

    @Override
    public void generateUnformatted(GenerateContext context) {
        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();

        ResultHandle intValueHandle = bytecodeCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Long.class, "longValue", long.class),
                context.getCurrentItem());

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "write", JsonGenerator.class, long.class),
                context.getJsonGenerator(),
                intValueHandle);
    }

}
