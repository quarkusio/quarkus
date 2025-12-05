package io.quarkus.it.hibernate.processor.data.pudefault;

import java.util.List;

import org.hibernate.annotations.processing.HQL;

import io.quarkus.security.PermissionsAllowed;

public interface SecuredHqlMethodRepository {

    @PermissionsAllowed("find")
    @HQL("WHERE name = :name")
    List<MyEntity> findByName(String name);

}
