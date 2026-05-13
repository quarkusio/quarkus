package io.quarkus.hibernate.panache.deployment.test.security.entities;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.panache.stateless.blocking.PanacheStatelessBlockingRepository;
import io.quarkus.security.PermissionsAllowed;

public interface StatelessBlockingMethodSecuredRepo extends PanacheStatelessBlockingRepository<StandaloneRepoEntity> {

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
