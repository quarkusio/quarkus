package io.quarkus.it.panache;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class NamedQueryRepository implements PanacheRepository<NamedQueryEntity> {
}
