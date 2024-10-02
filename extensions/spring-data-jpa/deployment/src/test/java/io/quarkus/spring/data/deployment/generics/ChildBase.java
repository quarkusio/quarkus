package io.quarkus.spring.data.deployment.generics;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class ChildBase {
    String nombre;
    String detail;
}
