package io.quarkus.it.corestuff.serialization;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = { List.class, ArrayList.class, String.class }, serialization = true)
public class SerializationConfig {

}
