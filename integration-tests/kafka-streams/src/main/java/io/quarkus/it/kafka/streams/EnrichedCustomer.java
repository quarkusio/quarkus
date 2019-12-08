package io.quarkus.it.kafka.streams;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class EnrichedCustomer {

    public int id;
    public String name;
    public Category category;

    public EnrichedCustomer() {
    }

    public EnrichedCustomer(int id, String name, Category category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }
}
