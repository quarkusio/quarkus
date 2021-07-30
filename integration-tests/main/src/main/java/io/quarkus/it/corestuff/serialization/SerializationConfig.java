package io.quarkus.it.corestuff.serialization;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = String.class, serialization = true)
public class SerializationConfig {

}
