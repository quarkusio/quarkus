package io.quarkus.it.hibernate.validator.orm;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.uuid.UuidGenerator;

@Entity
public class TestEntity {

    @Id
    @GeneratedValue
    @GenericGenerator(type = UuidGenerator.class)
    String id;

    @NotNull
    String validatedField;

}
