package io.quarkus.it.panache;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
@Transactional
public class BeerRepository implements PanacheRepository<Beer> {
    public List<Beer> findOrdered() {
        return find("ORDER BY name").list();
    }
}
