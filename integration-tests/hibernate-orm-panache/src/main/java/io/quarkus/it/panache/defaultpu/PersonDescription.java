package io.quarkus.it.panache.defaultpu;

import jakarta.persistence.Embeddable;

@Embeddable
public class PersonDescription {
    public Integer size;
    public Integer weight;
}
