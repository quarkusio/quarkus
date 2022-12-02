package io.quarkus.it.kafka.codecs;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.kafka.client.serialization.JsonbDeserializer;

public class PersonListDeserializer extends JsonbDeserializer<List<Person>> {
    public PersonListDeserializer() {
        super(new ArrayList<Person>() {
        }.getClass().getGenericSuperclass());
    }
}
