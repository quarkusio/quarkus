package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.resteasy.jsonb.deployment.SerializationClassInspector;

public final class TypeSerializerGeneratorRegistry {

    private final TypeSerializerGenerator objectSerializer = new ObjectTypeSerializerGenerator();

    private final List<TypeSerializerGenerator> typeSerializerGenerators = Arrays.asList(new StringTypeSerializerGenerator(),
            new PrimitiveIntTypeSerializerGenerator(), new PrimitiveLongTypeSerializerGenerator(),
            new PrimitiveBooleanTypeSerializerGenerator(),
            new BooleanTypeSerializerGenerator(), new IntegerTypeSerializerGenerator(), new LongTypeSerializerGenerator(),
            new BigDecimalTypeSerializerGenerator(),
            new LocalDateTimeSerializerGenerator(),
            objectSerializer,
            new ObjectArrayTypeSerializerGenerator(), new CollectionTypeSerializerGenerator(),
            new MapTypeSerializerGenerator(), new OptionalTypeSerializerGenerator());

    private final SerializationClassInspector inspector;

    public TypeSerializerGeneratorRegistry(SerializationClassInspector inspector) {
        this.inspector = inspector;
    }

    public TypeSerializerGenerator correspondingTypeSerializer(Type type) {
        for (TypeSerializerGenerator typeSerializerGenerator : typeSerializerGenerators) {
            if (typeSerializerGenerator.supports(type, this) != TypeSerializerGenerator.Supported.UNSUPPORTED) {
                return typeSerializerGenerator;
            }
        }
        return null;
    }

    public IndexView getIndex() {
        return inspector.getIndex();
    }

    public SerializationClassInspector getInspector() {
        return inspector;
    }

    public TypeSerializerGenerator getObjectSerializer() {
        return objectSerializer;
    }
}
