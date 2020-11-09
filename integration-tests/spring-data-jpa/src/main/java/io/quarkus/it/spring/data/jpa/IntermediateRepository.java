package io.quarkus.it.spring.data.jpa;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IntermediateRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    default public void doNothing() {
    }

    default public T findMandatoryById(ID id) {
        return findById(id).orElseThrow(() -> new IllegalStateException("not found: " + id));
    }

}
