package io.quarkus.confluent.registry.json;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.SpecificationVersion;

@TargetClass(className = "io.confluent.kafka.schemaregistry.json.JsonSchemaUtils")
final class Target_io_confluent_kafka_schemaregistry_json_JsonSchemaUtils {

    @Substitute
    public static JsonSchema getSchema(
            Object object,
            SpecificationVersion specVersion,
            boolean useOneofForNullables,
            boolean failUnknownProperties,
            ObjectMapper objectMapper,
            SchemaRegistryClient client) throws IOException {

        if (object == null) {
            return null;
        }

        Class<?> cls = object.getClass();
        //We only support the scenario of having the schema defined in the annotation in the java bean, since it does not rely on outdated libraries.
        if (cls.isAnnotationPresent(Schema.class)) {
            Schema schema = cls.getAnnotation(Schema.class);
            List<SchemaReference> references = Arrays.stream(schema.refs())
                    .map(new Function<io.confluent.kafka.schemaregistry.annotations.SchemaReference, SchemaReference>() {
                        @Override
                        public SchemaReference apply(
                                io.confluent.kafka.schemaregistry.annotations.SchemaReference schemaReference) {
                            return new SchemaReference(schemaReference.name(), schemaReference.subject(),
                                    schemaReference.version());
                        }
                    })
                    .collect(Collectors.toList());
            if (client == null) {
                if (!references.isEmpty()) {
                    throw new IllegalArgumentException("Cannot resolve schema " + schema.value()
                            + " with refs " + references);
                }
                return new JsonSchema(schema.value());
            } else {
                return (JsonSchema) client.parseSchema(JsonSchema.TYPE, schema.value(), references)
                        .orElseThrow(new Supplier<IOException>() {
                            @Override
                            public IOException get() {
                                return new IOException("Invalid schema " + schema.value()
                                        + " with refs " + references);
                            }
                        });
            }
        }
        return null;
    }
}

class ConfluentJsonSubstitutions {
}
