package io.quarkus.hibernate.orm.multitenancy.discriminator;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Drives persistence inside an active request context so that {@code HibernateCurrentTenantIdentifierResolver}
 * (which only resolves a tenant when a request context is active) consults the {@link LongTenantResolver}.
 */
@ApplicationScoped
public class ProductService {

    @Inject
    EntityManager entityManager;

    @Transactional
    @ActivateRequestContext
    public void create(String name) {
        entityManager.persist(new Product(name));
    }

    @Transactional
    @ActivateRequestContext
    public List<String> listNames() {
        return entityManager.createQuery("select p.name from Product p order by p.name", String.class)
                .getResultList();
    }

    @Transactional
    @ActivateRequestContext
    public Long tenantIdOfSingleProduct() {
        return entityManager.createQuery("select p.tenantId from Product p", Long.class)
                .setMaxResults(1)
                .getSingleResult();
    }
}
