package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.data.hibernate.RecordRepository;
import io.quarkus.security.PermissionsAllowed;

public interface StatelessBlockingMethodSecuredRepo extends RecordRepository<StandaloneRepoEntity> {

    @RolesAllowed("admin")
    @Find
    List<StandaloneRepoEntity> findByName(String name);

    @PermissionsAllowed("admin")
    @PermissionsAllowed("root")
    @HQL("where name = :name")
    List<StandaloneRepoEntity> hqlByName(String name);

    @Find
    List<StandaloneRepoEntity> unsecuredFindByName(String name);
}
