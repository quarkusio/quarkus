package io.quarkus.example.jpaoracle.procedurecall;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity(name = ProcedureCallEntity.NAME)
public class ProcedureCallEntity {

    public static final String NAME = "procentity";

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public ProcedureCallEntity() {
    }

    public ProcedureCallEntity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
