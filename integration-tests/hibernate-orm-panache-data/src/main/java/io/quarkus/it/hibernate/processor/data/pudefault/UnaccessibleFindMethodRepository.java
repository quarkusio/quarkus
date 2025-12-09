package io.quarkus.it.hibernate.processor.data.pudefault;

import java.util.List;

import jakarta.annotation.security.DenyAll;

import org.hibernate.annotations.processing.Find;

/**
 * Tests that class-level security annotation is applied on interface methods.
 */
@DenyAll
public interface UnaccessibleFindMethodRepository {

    @Find
    List<MyEntity> findByName(String name);

}
