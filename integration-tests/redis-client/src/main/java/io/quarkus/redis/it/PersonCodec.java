package io.quarkus.redis.it;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.redis.datasource.codecs.Codec;

@ApplicationScoped
public class PersonCodec implements Codec {
    @Override
    public boolean canHandle(Type clazz) {
        return clazz.equals(Person.class);
    }

    @Override
    public byte[] encode(Object item) {
        var p = (Person) item;
        return (p.firstName + ";" + p.lastName.toUpperCase()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object decode(byte[] item) {
        var value = new String(item, StandardCharsets.UTF_8);
        var segments = value.split(";");
        return new Person(segments[0], segments[1]);
    }
}
