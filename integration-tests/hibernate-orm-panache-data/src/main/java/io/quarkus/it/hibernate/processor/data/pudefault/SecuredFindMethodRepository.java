package io.quarkus.it.hibernate.processor.data.pudefault;

import java.util.List;

import org.hibernate.annotations.processing.Find;

import io.quarkus.security.PermissionsAllowed;

public interface SecuredFindMethodRepository {

    @PermissionsAllowed("find")
    @Find
    List<MyEntity> findByName(String name);

}
