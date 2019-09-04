package io.quarkus.resteasy.jsonb.deployment.serializers;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

import org.eclipse.yasson.internal.Marshaller;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.runtime.serializers.UnhandledTypeGeneratorUtil;

/**
 * Generator that simply delegates to Yasson
 *
 * Important notes:
 * 1) Results in reflection being done on the enclosing type in order to populate the metadata needed by Yasson
 * 2) Doesn't handle generic types
 */
public class UnhandledTypeGenerator extends AbstractTypeSerializerGenerator {

    private final Type enclosingType;
    private final String propertyName;

    UnhandledTypeGenerator(Type enclosingType, String propertyName) {
        this.enclosingType = enclosingType;
        this.propertyName = propertyName;
    }

    // won't ever be called because this class isn't part of the TypeSerializerGeneratorRegistry
    // it is instead constructed on demand by ObjectTypeSerializerGenerator
    @Override
    public Supported supports(Type type, TypeSerializerGeneratorRegistry registry) {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    protected void generateNotNull(GenerateContext context) {
        // we generate pretty much the same code as Yasson's ObjectSerializer#marshallProperty

        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();
        ResultHandle jsonGenerator = context.getJsonGenerator();
        ResultHandle serializationContext = context.getSerializationContext();

        ResultHandle marshaller = bytecodeCreator.checkCast(serializationContext, Marshaller.class);

        ResultHandle propertyCachedSerializer = bytecodeCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(UnhandledTypeGeneratorUtil.class, "getSerializerForUnhandledType",
                        JsonbSerializer.class, Marshaller.class, Class.class, Object.class, String.class),
                marshaller, bytecodeCreator.loadClass(enclosingType.name().toString()),
                context.getCurrentItem(), bytecodeCreator.load(propertyName));

        bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonbSerializer.class, "serialize", void.class, Object.class, JsonGenerator.class,
                        SerializationContext.class),
                propertyCachedSerializer, context.getCurrentItem(), jsonGenerator, context.getSerializationContext());
    }

}
