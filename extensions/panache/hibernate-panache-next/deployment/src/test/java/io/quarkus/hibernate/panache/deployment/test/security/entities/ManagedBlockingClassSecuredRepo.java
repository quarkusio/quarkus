package io.quarkus.hibernate.panache.deployment.test.security.entities;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import org.hibernate.annotations.processing.Find;

import io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingRepository;

@RolesAllowed("admin")
public interface ManagedBlockingClassSecuredRepo extends PanacheManagedBlockingRepository<StandaloneRepoEntity> {

    @Find
    List<StandaloneRepoEntity> findByName(String name);
}
