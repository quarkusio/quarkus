package io.quarkus.spring.data.deployment.generics;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentBaseRepository<T extends ParentBase<?>> extends JpaRepository<T, Long> {
    long countParentsByChildren_Nombre(String name);
}
