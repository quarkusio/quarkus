package io.quarkus.it.kafka;

import io.quarkus.kafka.client.serialization.JsonbSerializer;

public class PersonSerializer extends JsonbSerializer<Person> {
}
