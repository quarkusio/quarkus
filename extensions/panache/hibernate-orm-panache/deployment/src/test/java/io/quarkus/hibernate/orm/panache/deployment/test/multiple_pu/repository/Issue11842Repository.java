package io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.repository;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class Issue11842Repository implements PanacheRepositoryBase<Issue11842Entity, Integer> {
}
