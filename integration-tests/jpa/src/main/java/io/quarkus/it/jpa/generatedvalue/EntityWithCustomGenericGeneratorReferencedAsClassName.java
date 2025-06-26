package io.quarkus.it.jpa.generatedvalue;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class EntityWithCustomGenericGeneratorReferencedAsClassName {
    @Id
    @GeneratedValue(generator = "referenced-as-class-name")
    @GenericGenerator(name = "referenced-as-class-name", strategy = "io.quarkus.it.jpa.generatedvalue.MyCustomGenericGeneratorReferencedAsClassName")
    public String id;
}
