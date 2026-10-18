package io.quarkus.it.jpa.generatedvalue;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class EntityWithCustomGenericGeneratorReferencedAsClassName {
    @Id
    @GeneratedValue
    @GenericGenerator(type = MyCustomGenericGeneratorReferencedAsClassName.class)
    public String id;
}
