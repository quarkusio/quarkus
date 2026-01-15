package io.quarkus.bootstrap.json;

public sealed interface JsonValue permits JsonBoolean, JsonMember, JsonMultiValue, JsonNull, JsonNumber, JsonString {
}
