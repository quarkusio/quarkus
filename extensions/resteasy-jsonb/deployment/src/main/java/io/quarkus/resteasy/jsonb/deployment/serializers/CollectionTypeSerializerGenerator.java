package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.util.Collection;
import java.util.Iterator;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.Type;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.deployment.CollectionUtil;

public class CollectionTypeSerializerGenerator extends AbstractTypeSerializerGenerator {

    @Override
    public Supported supports(Type type, TypeSerializerGeneratorRegistry registry) {
        if (!CollectionUtil.isCollection(type.name())) {
            return Supported.UNSUPPORTED;
        }

        Type genericType = CollectionUtil.getGenericType(type);
        if (genericType == null) {
            return Supported.UNSUPPORTED;
        }

        TypeSerializerGenerator typeSerializerGenerator = registry.correspondingTypeSerializer(genericType);
        return typeSerializerGenerator != null ? typeSerializerGenerator.supports(genericType, registry)
                : Supported.UNSUPPORTED;
    }

    @Override
    public void generateNotNull(GenerateContext context) {
        Type genericType = CollectionUtil.getGenericType(context.getType());
        if (genericType == null) {
            throw new IllegalStateException("Could not generate serializer for collection type " + context.getType());
        }

        TypeSerializerGenerator genericTypeSerializerGenerator = context.getRegistry().correspondingTypeSerializer(genericType);
        if (genericTypeSerializerGenerator == null) {
            throw new IllegalStateException("Could not generate serializer for generic type " + genericType.name() +
                    " of collection type" + context.getType().name());
        }

        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();
        ResultHandle jsonGenerator = context.getJsonGenerator();

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "writeStartArray", JsonGenerator.class),
                jsonGenerator);

        ResultHandle iterator = bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Collection.class, "iterator", Iterator.class),
                context.getCurrentItem());

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
        genericTypeSerializerGenerator.generate(context.changeItem(hasNextBranch, genericType, next, false));
        hasNextBranch.continueScope(loop);

        noNextBranch.breakScope(loop);

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "writeEnd", JsonGenerator.class), jsonGenerator);
    }

}
