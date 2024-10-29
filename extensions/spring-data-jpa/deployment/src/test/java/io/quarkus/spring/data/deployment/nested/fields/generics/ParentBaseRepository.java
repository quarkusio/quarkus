package io.quarkus.spring.data.deployment.nested.fields.generics;

import org.springframework.data.jpa.repository.JpaRepository;

// Issue 34395: this repo is used in MethodNameParserTest
public interface ParentBaseRepository<T extends ParentBase<?>> extends JpaRepository<T, Long> {
    long countParentsByChildren_Nombre(String name);
}
