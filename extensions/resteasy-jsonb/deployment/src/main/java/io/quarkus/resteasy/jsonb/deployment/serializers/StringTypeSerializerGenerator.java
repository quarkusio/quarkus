package io.quarkus.resteasy.jsonb.deployment.serializers;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.Type;

import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.resteasy.jsonb.deployment.DotNames;

public class StringTypeSerializerGenerator extends AbstractTypeSerializerGenerator {

    @Override
    public boolean supports(Type type, TypeSerializerGeneratorRegistry registry) {
        return DotNames.STRING.equals(type.name());
    }

    @Override
    protected void generateNotNull(GenerateContext context) {
        context.getBytecodeCreator().invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "write", JsonGenerator.class, String.class),
                context.getJsonGenerator(),
                context.getCurrentItem());
    }

}
