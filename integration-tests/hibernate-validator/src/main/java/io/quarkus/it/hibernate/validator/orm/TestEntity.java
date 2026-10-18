package io.quarkus.it.hibernate.validator.orm;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.UuidGenerator;

@Entity
public class TestEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    String id;

    @NotNull
    String validatedField;

}
