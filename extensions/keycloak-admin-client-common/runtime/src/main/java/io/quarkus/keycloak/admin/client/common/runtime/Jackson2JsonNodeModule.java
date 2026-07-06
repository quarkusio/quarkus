package io.quarkus.keycloak.admin.client.common.runtime;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Bridge module that teaches Jackson 3 how to serialize and deserialize
 * Jackson 2's {@code com.fasterxml.jackson.databind.JsonNode}.
 * <p>
 * Needed because Keycloak's representation classes (e.g. RealmRepresentation)
 * use Jackson 2 JsonNode as field types, but Quarkus now uses Jackson 3.
 * <p>
 * TODO: Remove once Keycloak ships a Jackson 3 compatible version.
 */
public class Jackson2JsonNodeModule extends SimpleModule {

    public Jackson2JsonNodeModule() {
        super("jackson2-jsonnode-bridge");
        addSerializer(com.fasterxml.jackson.databind.JsonNode.class, new Jackson2JsonNodeSerializer());
        addDeserializer(com.fasterxml.jackson.databind.JsonNode.class, new Jackson2JsonNodeDeserializer());
    }

    static final class Jackson2JsonNodeSerializer extends ValueSerializer<com.fasterxml.jackson.databind.JsonNode> {

        @Override
        public void serialize(com.fasterxml.jackson.databind.JsonNode value, JsonGenerator gen,
                SerializationContext serializers) throws JacksonException {
            writeNode(value, gen);
        }

        private void writeNode(com.fasterxml.jackson.databind.JsonNode node, JsonGenerator gen) throws JacksonException {
            if (node.isObject()) {
                gen.writeStartObject();
                Set<Map.Entry<String, JsonNode>> fields = node.properties();
                for (Map.Entry<String, JsonNode> field : fields) {
                    gen.writeName(field.getKey());
                    writeNode(field.getValue(), gen);
                }
                gen.writeEndObject();
            } else if (node.isArray()) {
                gen.writeStartArray();
                for (com.fasterxml.jackson.databind.JsonNode element : node) {
                    writeNode(element, gen);
                }
                gen.writeEndArray();
            } else if (node.isTextual()) {
                gen.writeString(node.textValue());
            } else if (node.isNumber()) {
                if (node.isIntegralNumber()) {
                    if (node.isBigInteger()) {
                        gen.writeNumber(node.bigIntegerValue());
                    } else {
                        gen.writeNumber(node.longValue());
                    }
                } else if (node.isBigDecimal()) {
                    gen.writeNumber(node.decimalValue());
                } else {
                    gen.writeNumber(node.doubleValue());
                }
            } else if (node.isBoolean()) {
                gen.writeBoolean(node.booleanValue());
            } else if (node.isNull()) {
                gen.writeNull();
            } else if (node.isBinary()) {
                try {
                    gen.writeBinary(node.binaryValue());
                } catch (java.io.IOException e) {
                    throw tools.jackson.core.exc.JacksonIOException.construct(e);
                }
            }
        }
    }

    static final class Jackson2JsonNodeDeserializer extends ValueDeserializer<com.fasterxml.jackson.databind.JsonNode> {

        @Override
        public com.fasterxml.jackson.databind.JsonNode deserialize(JsonParser p, DeserializationContext ctxt)
                throws JacksonException {
            return readNode(p);
        }

        private com.fasterxml.jackson.databind.JsonNode readNode(JsonParser p) throws JacksonException {
            return switch (p.currentToken()) {
                case START_OBJECT -> readObject(p);
                case START_ARRAY -> readArray(p);
                case VALUE_STRING -> com.fasterxml.jackson.databind.node.TextNode.valueOf(p.getString());
                case VALUE_NUMBER_INT -> switch (p.getNumberType()) {
                    case BIG_INTEGER ->
                        com.fasterxml.jackson.databind.node.BigIntegerNode.valueOf(p.getBigIntegerValue());
                    case LONG -> com.fasterxml.jackson.databind.node.LongNode.valueOf(p.getLongValue());
                    default -> com.fasterxml.jackson.databind.node.IntNode.valueOf(p.getIntValue());
                };
                case VALUE_NUMBER_FLOAT -> {
                    if (p.getNumberType() == JsonParser.NumberType.BIG_DECIMAL) {
                        yield com.fasterxml.jackson.databind.node.DecimalNode.valueOf(p.getDecimalValue());
                    }
                    yield com.fasterxml.jackson.databind.node.DoubleNode.valueOf(p.getDoubleValue());
                }
                case VALUE_TRUE -> com.fasterxml.jackson.databind.node.BooleanNode.TRUE;
                case VALUE_FALSE -> com.fasterxml.jackson.databind.node.BooleanNode.FALSE;
                case VALUE_NULL -> com.fasterxml.jackson.databind.node.NullNode.getInstance();
                default -> throw new IllegalStateException("Unexpected token: " + p.currentToken());
            };
        }

        private com.fasterxml.jackson.databind.node.ObjectNode readObject(JsonParser p) throws JacksonException {
            com.fasterxml.jackson.databind.node.ObjectNode node = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                    .objectNode();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken();
                node.set(fieldName, readNode(p));
            }
            return node;
        }

        private com.fasterxml.jackson.databind.node.ArrayNode readArray(JsonParser p) throws JacksonException {
            com.fasterxml.jackson.databind.node.ArrayNode node = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                    .arrayNode();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                node.add(readNode(p));
            }
            return node;
        }
    }
}
