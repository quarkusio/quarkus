package io.quarkus.it.hibernate.processor.data.pudefault;

import java.util.List;

import org.hibernate.annotations.processing.SQL;

import io.quarkus.security.PermissionsAllowed;

public interface SecuredSqlMethodRepository {

    @PermissionsAllowed("find")
    @SQL("SELECT * FROM MyEntity WHERE name = :name")
    List<MyEntity> findByName(String name);

}
