package io.quarkus.it.spring.data.jpa.generics;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

@MappedSuperclass
public class FatherBase<T extends ChildBase> {
    String name;
    String detail;
    int age;
    float test;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<T> children;
}
