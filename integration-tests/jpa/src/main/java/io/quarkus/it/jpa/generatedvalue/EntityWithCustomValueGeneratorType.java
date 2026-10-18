package io.quarkus.it.jpa.generatedvalue;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class EntityWithCustomValueGeneratorType {
    @Id
    @GeneratedValue
    public Integer id;

    @MyCustomValueGeneratorAnnotation
    public String customGenerated;
}
