package io.quarkus.it.spring.data.jpa.generics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface FatherBaseRepository<T extends FatherBase<?>> extends JpaRepository<T, Long> {
    long countParentsByChildren_Name(String name);
}