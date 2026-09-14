package io.quarkus.hibernate.orm.multitenancy.database;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CompanyService {

    @Inject
    EntityManager entityManager;

    @Transactional
    @ActivateRequestContext
    public void create(String name) {
        entityManager.persist(new Company(name));
    }

    @Transactional
    @ActivateRequestContext
    public List<String> listNames() {
        return entityManager.createQuery("select c.name from Company c order by c.id", String.class)
                .getResultList();
    }
}
