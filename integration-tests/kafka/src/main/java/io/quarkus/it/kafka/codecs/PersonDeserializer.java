package io.quarkus.it.kafka.codecs;

import io.quarkus.kafka.client.serialization.JsonbDeserializer;

public class PersonDeserializer extends JsonbDeserializer<Person> {
    public PersonDeserializer() {
        super(Person.class);
    }
}
