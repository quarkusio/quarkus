package io.quarkus.it.spring.data.jpa.generics;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class ChildBase {
    String name;
    String detail;
}
