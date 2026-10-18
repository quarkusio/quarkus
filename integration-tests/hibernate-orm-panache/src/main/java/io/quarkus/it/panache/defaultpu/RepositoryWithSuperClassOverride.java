package io.quarkus.it.panache.defaultpu;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RepositoryWithSuperClassOverride extends AbstractDontDeletePanacheRepository<Person, Long> {
}
