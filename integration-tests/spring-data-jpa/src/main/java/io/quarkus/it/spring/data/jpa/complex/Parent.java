package io.quarkus.it.spring.data.jpa.complex;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Parent extends ParentBase {
    @Id
    private Long id;

    public Parent(Long id, String name, String detail, int age, float test, TestEnum testEnum) {
        super(name, detail, age, test, testEnum);
        this.id = id;
    }

    public Parent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
