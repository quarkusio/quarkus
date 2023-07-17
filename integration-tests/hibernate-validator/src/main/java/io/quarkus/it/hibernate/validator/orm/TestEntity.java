package io.quarkus.it.hibernate.validator.orm;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class TestEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    String id;

    @NotNull
    String validatedField;

}
