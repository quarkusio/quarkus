package io.quarkus.it.jpa.generatedvalue;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class EntityWithCustomIdGeneratorType {
    @Id
    @GeneratedValue
    @MyCustomIdGeneratorAnnotation
    public String id;
}
