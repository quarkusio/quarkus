package io.quarkus.it.jpa.generatedvalue;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class EntityWithCustomGenericGeneratorReferencedAsClass {
    @Id
    @GeneratedValue
    @GenericGenerator(type = MyCustomGenericGeneratorReferencedAsClass.class)
    public String id;
}
