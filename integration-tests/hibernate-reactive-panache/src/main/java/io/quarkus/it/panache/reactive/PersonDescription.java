package io.quarkus.it.panache.reactive;

import jakarta.persistence.Embeddable;

@Embeddable
public class PersonDescription {
    public Integer size;
    public Integer weight;
}
