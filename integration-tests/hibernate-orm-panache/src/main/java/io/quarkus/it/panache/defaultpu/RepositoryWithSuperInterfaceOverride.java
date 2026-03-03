package io.quarkus.it.panache.defaultpu;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RepositoryWithSuperInterfaceOverride implements DontDeletePanacheRepository<Person, Long> {
}
