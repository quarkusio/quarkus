package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.Dependent;

import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.pulsar.PulsarSchemaCreator;
import io.quarkus.pulsar.PulsarSchemaCreator.Kind;
import io.quarkus.runtime.util.HashUtil;
import io.smallrye.common.annotation.Identifier;

public class SyntheticBeanBuilder {
    final BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItem;
    final Map<String, String> alreadyGeneratedSchema;

    public SyntheticBeanBuilder(BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItem) {
        this.syntheticBeanBuildItem = syntheticBeanBuildItem;
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
            produceSyntheticBeanSchema(syntheticBeanBuildItem, Kind.OBJECT_MAPPER, schemaId, type);
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
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, Kind.AVRO, schemaId, type);
                } else if (discovery.isProtobufGenerated(type.name())) {
                    schemaId = generateId(type, "PROTOBUF");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, Kind.PROTOBUF, schemaId, type);
                } else if (type.name().equals(DotNames.VERTX_JSON_OBJECT)) {
                    schemaId = generateId(type, "JSON_OBJECT");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, Kind.JSON_OBJECT, schemaId, type);
                } else if (type.name().equals(DotNames.VERTX_JSON_ARRAY)) {
                    schemaId = generateId(type, "JSON_ARRAY");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, Kind.JSON_ARRAY, schemaId, type);
                } else if (type.name().equals(DotNames.VERTX_BUFFER)) {
                    schemaId = generateId(type, "BUFFER");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, Kind.BUFFER, schemaId, type);
                } else if (type.name().equals(DotNames.BYTE_BUFFER)) {
                    schemaId = generateId(type, "BYTE_BUFFER");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, Kind.BYTE_BUFFER, schemaId, type);
                } else {
                    schemaId = generateId(type, "JSON");
                    produceSyntheticBeanSchema(syntheticBeanBuildItem, Kind.JSON, schemaId, type);
                }
                alreadyGeneratedSchema.put(typeName, schemaId);
            }
            return schemaId;
        }
        return null;
    }

    void produceSyntheticBeanSchema(BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItem,
            Kind kind,
            String schemaId,
            Type type) {
        ParameterizedType providerType = ParameterizedType.create(DotNames.PULSAR_SCHEMA, type);
        syntheticBeanBuildItem.produce(SyntheticBeanBuildItem.configure(Object.class)
                .providerType(providerType)
                .addType(providerType)
                .addQualifier().annotation(Identifier.class).addValue("value", schemaId).done()
                .scope(Dependent.class)
                .creator(PulsarSchemaCreator.class)
                .param(PulsarSchemaCreator.KIND, kind)
                .param(PulsarSchemaCreator.TYPE, type.name().toString())
                .unremovable()
                .done());
    }

    String generateId(Type type, String targetType) {
        String baseName = type.name().withoutPackagePrefix();
        return baseName + "_" + targetType + "Schema_"
                + HashUtil.sha1(type.name().toString());
    }
}
