package io.quarkus.it.kafka.streams.norocksdb;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Message {

    public String key;
    public String value;

    public Message() {
    }

    public Message(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
