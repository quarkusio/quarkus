package io.quarkus.it.corestuff.serialization;

import java.util.ArrayList;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = { ArrayList.class, String.class }, serialization = true)
public class SerializationConfig {

}
