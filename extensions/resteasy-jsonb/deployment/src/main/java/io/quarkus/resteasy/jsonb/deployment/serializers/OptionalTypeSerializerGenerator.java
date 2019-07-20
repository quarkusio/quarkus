package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.util.List;
import java.util.Optional;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.deployment.DotNames;

public class OptionalTypeSerializerGenerator extends AbstractTypeSerializerGenerator {

    @Override
    public Supported supports(Type type, TypeSerializerGeneratorRegistry registry) {
        if (!DotNames.OPTIONAL.equals(type.name())) {
            return Supported.UNSUPPORTED;
        }

        if (!(type instanceof ParameterizedType)) {
            return Supported.UNSUPPORTED;
        }

        List<Type> typeArguments = type.asParameterizedType().arguments();
        if (typeArguments.size() != 1) {
            return Supported.UNSUPPORTED;
        }

        Type genericType = typeArguments.get(0);
        TypeSerializerGenerator typeSerializerGenerator = registry.correspondingTypeSerializer(genericType);
        return typeSerializerGenerator != null ? typeSerializerGenerator.supports(genericType, registry)
                : Supported.UNSUPPORTED;
    }

    @Override
    protected void generateNotNull(GenerateContext context) {
        if (!(context.getType() instanceof ParameterizedType)) {
            throw new IllegalStateException("Could not generate serializer for type " + context.getType());
        }

        List<Type> arguments = context.getType().asParameterizedType().arguments();
        if (arguments.size() != 1) {
            throw new IllegalStateException(
                    "Could not generate serializer for type " + context.getType() + " with generic arguments" + arguments);
        }

        Type genericType = arguments.get(0);
        TypeSerializerGenerator genericTypeSerializerGenerator = context.getRegistry()
                .correspondingTypeSerializer(genericType);
        if (genericTypeSerializerGenerator == null) {
            throw new IllegalStateException("Could not generate serializer for generic type " + genericType.name() +
                    " of collection type" + context.getType().name());
        }

        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();
        if (context.isNullChecked()) {
            doSerialize(context, genericType, genericTypeSerializerGenerator, bytecodeCreator);
        } else {
            ResultHandle isPresent = bytecodeCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Optional.class, "isPresent", boolean.class),
                    context.getCurrentItem());

            BytecodeCreator ifScope = bytecodeCreator.createScope();
            BranchResult isPresentBranch = ifScope.ifNonZero(isPresent);
            BytecodeCreator isPresentFalse = isPresentBranch.falseBranch();
            isPresentFalse.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(JsonGenerator.class, "writeNull", JsonGenerator.class),
                    context.getJsonGenerator());
            isPresentFalse.breakScope(ifScope);

            BytecodeCreator isPresentTrue = isPresentBranch.trueBranch();
            doSerialize(context, genericType, genericTypeSerializerGenerator, isPresentTrue);
            isPresentTrue.breakScope(ifScope);
        }
    }

    private void doSerialize(GenerateContext context, Type genericType, TypeSerializerGenerator genericTypeSerializerGenerator,
            BytecodeCreator isPresentTrue) {
        ResultHandle item = isPresentTrue.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Optional.class, "get", Object.class),
                context.getCurrentItem());
        genericTypeSerializerGenerator.generate(context.changeItem(isPresentTrue, genericType, item, true));
    }
}
