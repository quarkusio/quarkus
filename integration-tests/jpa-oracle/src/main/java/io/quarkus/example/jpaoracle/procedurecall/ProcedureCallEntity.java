package io.quarkus.example.jpaoracle.procedurecall;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
