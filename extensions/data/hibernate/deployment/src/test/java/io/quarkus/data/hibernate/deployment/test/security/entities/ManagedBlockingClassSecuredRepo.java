package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import org.hibernate.annotations.processing.Find;

import io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingRepository;

@RolesAllowed("admin")
public interface ManagedBlockingClassSecuredRepo extends PanacheManagedBlockingRepository<StandaloneRepoEntity> {

    @Find
    List<StandaloneRepoEntity> findByName(String name);
}
