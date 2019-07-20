package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class ObjectArrayTypeSerializerGenerator extends AbstractTypeSerializerGenerator {

    @Override
    public Supported supports(Type type, TypeSerializerGeneratorRegistry registry) {
        if (!(type instanceof ArrayType)) {
            return Supported.UNSUPPORTED;
        }

        Type componentType = type.asArrayType().component();
        if (componentType instanceof PrimitiveType) {
            return Supported.UNSUPPORTED;
        }

        TypeSerializerGenerator typeSerializerGenerator = registry.correspondingTypeSerializer(componentType);
        return typeSerializerGenerator != null ? typeSerializerGenerator.supports(componentType, registry)
                : Supported.UNSUPPORTED;
    }

    @Override
    public void generateNotNull(GenerateContext context) {
        if (!(context.getType() instanceof ArrayType)) {
            throw new IllegalStateException(context.getType().name() + " is not an array type");
        }

        Type componentType = context.getType().asArrayType().component();
        TypeSerializerGenerator genericTypeSerializerGenerator = context.getRegistry()
                .correspondingTypeSerializer(componentType);
        if (genericTypeSerializerGenerator == null) {
            throw new IllegalStateException("Could not generate serializer for generic type " + componentType.name() +
                    " of collection type" + context.getType().name());
        }

        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();
        ResultHandle jsonGenerator = context.getJsonGenerator();

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "writeStartArray", JsonGenerator.class),
                jsonGenerator);

        ResultHandle asList = bytecodeCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(Arrays.class, "asList", List.class, Object[].class),
                context.getCurrentItem());

        ResultHandle iterator = bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(List.class, "iterator", Iterator.class),
                asList);

        BytecodeCreator loop = bytecodeCreator.createScope();

        ResultHandle hasNext = loop.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class),
                iterator);
        BranchResult branchResult = loop.ifNonZero(hasNext);
        BytecodeCreator hasNextBranch = branchResult.trueBranch();
        BytecodeCreator noNextBranch = branchResult.falseBranch();
        ResultHandle next = hasNextBranch.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Iterator.class, "next", Object.class),
                iterator);
        genericTypeSerializerGenerator.generate(context.changeItem(hasNextBranch, componentType, next, false));
        hasNextBranch.continueScope(loop);

        noNextBranch.breakScope(loop);

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "writeEnd", JsonGenerator.class), jsonGenerator);
    }

}
