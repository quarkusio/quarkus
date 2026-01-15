package io.quarkus.bootstrap.json;

public sealed interface JsonNumber extends JsonValue permits JsonDouble, JsonInteger {
}
