package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.Type;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.deployment.DotNames;
import io.quarkus.resteasy.jsonb.deployment.MapUtil;

public class MapTypeSerializerGenerator extends AbstractTypeSerializerGenerator {
    @Override
    public Supported supports(Type type, TypeSerializerGeneratorRegistry registry) {
        if (!MapUtil.isMap(type.name())) {
            return Supported.UNSUPPORTED;
        }

        MapUtil.MapTypes genericTypes = MapUtil.getGenericType(type);
        if (genericTypes == null) {
            return Supported.UNSUPPORTED;
        }

        if (!DotNames.STRING.equals(genericTypes.getKeyType().name())) {
            return Supported.UNSUPPORTED;
        }

        TypeSerializerGenerator typeSerializerGenerator = registry.correspondingTypeSerializer(genericTypes.getValueType());
        return typeSerializerGenerator != null ? typeSerializerGenerator.supports(genericTypes.getValueType(), registry)
                : Supported.UNSUPPORTED;
    }

    @Override
    protected void generateNotNull(GenerateContext context) {
        MapUtil.MapTypes genericTypes = MapUtil.getGenericType(context.getType());
        if (genericTypes == null) {
            throw new IllegalStateException("Could not generate serializer for collection type " + context.getType());
        }

        Type genericTypeOfValue = genericTypes.getValueType();
        TypeSerializerGenerator genericTypeSerializerGenerator = context.getRegistry()
                .correspondingTypeSerializer(genericTypeOfValue);
        if (genericTypeSerializerGenerator == null) {
            throw new IllegalStateException("Could not generate serializer for generic type " + genericTypeOfValue.name() +
                    " of collection type" + context.getType().name());
        }

        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();
        ResultHandle jsonGenerator = context.getJsonGenerator();

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "writeStartObject", JsonGenerator.class),
                jsonGenerator);

        ResultHandle entrySet = bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Map.class, "entrySet", Set.class),
                context.getCurrentItem());
        ResultHandle iterator = bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Set.class, "iterator", Iterator.class),
                entrySet);

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
        ResultHandle key = hasNextBranch.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Map.Entry.class, "getKey", Object.class),
                next);
        ResultHandle value = hasNextBranch.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Map.Entry.class, "getValue", Object.class),
                next);

        ResultHandle keyAsString = hasNextBranch.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Object.class, "toString", String.class),
                key);
        hasNextBranch.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "writeKey", JsonGenerator.class, String.class),
                jsonGenerator, keyAsString);
        genericTypeSerializerGenerator.generate(context.changeItem(hasNextBranch, genericTypeOfValue, value, false));

        hasNextBranch.continueScope(loop);

        noNextBranch.breakScope(loop);

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "writeEnd", JsonGenerator.class), jsonGenerator);
    }
}
