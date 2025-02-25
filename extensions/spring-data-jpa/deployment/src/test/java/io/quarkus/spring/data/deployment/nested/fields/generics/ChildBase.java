package io.quarkus.spring.data.deployment.nested.fields.generics;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class ChildBase {
    String nombre;
    String detail;
}
