package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.enterprise.context.Dependent;

import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.pulsar.SchemaProviderRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.util.HashUtil;
import io.smallrye.common.annotation.Identifier;

public class SyntheticBeanBuilder {
    final BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItem;
    final SchemaProviderRecorder recorder;
    final RecorderContext recorderContext;
    final Map<String, String> alreadyGeneratedSchema;

    public SyntheticBeanBuilder(BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItem,
            SchemaProviderRecorder recorder,
            RecorderContext recorderContext) {
        this.syntheticBeanBuildItem = syntheticBeanBuildItem;
        this.recorder = recorder;
        this.recorderContext = recorderContext;
        this.alreadyGeneratedSchema = new HashMap<>();
    }

    static String objectMapperSchemaId(Type type) {
        return "ObjectMapper<" + type.name() + ">";
    }

    String schemaIdFor(Type type) {
        return alreadyGeneratedSchema.get(type.name().toString());
    }

    void produceObjectMapperSchemaBean(String schemaId, Type type) {
        String typeName = type.name().toString();
        if (!alreadyGeneratedSchema.containsKey(typeName)
                || alreadyGeneratedSchema.get(typeName).equals(schemaId)) {
            var runtimeValue = recorder.createObjectMapperSchema(recorderContext.classProxy(typeName));
            produceSyntheticBeanSchema(syntheticBeanBuildItem, runtimeValue, schemaId, type);
            alreadyGeneratedSchema.put(typeName, schemaId);
        }
    }

    public String produceSchemaBean(DefaultSchemaDiscoveryState discovery, Type type) {
        if (syntheticBeanBuildItem != null && type.kind() == Type.Kind.CLASS) {
            String schemaId = schemaIdFor(type);
            if (schemaId == null) {
                String typeName = type.name().toString();
                if (discovery.isAvroGenerated(type.name()) || DotNames.AVRO_GENERIC_RECORD.equals(type.name())) {
                    schemaId = generateId(type, "AVRO");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem,
                            recorder.createAvroSchema(recorderContext.classProxy(typeName)), schemaId, type);
                } else if (discovery.isProtobufGenerated(type.name())) {
                    schemaId = generateId(type, "PROTOBUF");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem,
                            recorder.createProtoBufSchema(recorderContext.classProxy(typeName)), schemaId, type);
                } else if (type.name().equals(DotNames.VERTX_JSON_OBJECT)) {
                    schemaId = generateId(type, "JSON_OBJECT");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, recorder.createJsonObjectSchema(), schemaId, type);
                } else if (type.name().equals(DotNames.VERTX_JSON_ARRAY)) {
                    schemaId = generateId(type, "JSON_ARRAY");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, recorder.createJsonArraySchema(), schemaId, type);
                } else if (type.name().equals(DotNames.VERTX_BUFFER)) {
                    schemaId = generateId(type, "BUFFER");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, recorder.createBufferSchema(), schemaId, type);
                } else if (type.name().equals(DotNames.BYTE_BUFFER)) {
                    schemaId = generateId(type, "BYTE_BUFFER");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, recorder.createByteBufferSchema(), schemaId, type);
                } else {
                    schemaId = generateId(type, "JSON");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem,
                            recorder.createJsonSchema(recorderContext.classProxy(typeName)), schemaId, type);
                }
                alreadyGeneratedSchema.put(typeName, schemaId);
            }
            return schemaId;
        }
        return null;
    }

    void produceSyntheticBeanSchema(BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItem,
            RuntimeValue<?> runtimeValue,
            String schemaId,
            Type type) {
        ParameterizedType providerType = ParameterizedType.create(DotNames.PULSAR_SCHEMA, type);
        syntheticBeanBuildItem.produce(SyntheticBeanBuildItem.configure(Object.class)
                .providerType(providerType)
                .addType(providerType)
                .addQualifier().annotation(Identifier.class).addValue("value", schemaId).done()
                .scope(Dependent.class)
                .runtimeValue(runtimeValue)
                .unremovable()
                .done());
    }

    void produceSyntheticBeanSchema(BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItem,
            Supplier<?> supplier,
            String schemaId,
            Type type) {
        ParameterizedType providerType = ParameterizedType.create(DotNames.PULSAR_SCHEMA, type);
        syntheticBeanBuildItem.produce(SyntheticBeanBuildItem.configure(Object.class)
                .providerType(providerType)
                .addType(providerType)
                .addQualifier().annotation(Identifier.class).addValue("value", schemaId).done()
                .scope(Dependent.class)
                .supplier(supplier)
                .unremovable()
                .done());
    }

    String generateId(Type type, String targetType) {
        String baseName = type.name().withoutPackagePrefix();
        return baseName + "_" + targetType + "Schema_"
                + HashUtil.sha1(Long.toString(UUID.randomUUID().getMostSignificantBits()));
    }
}
