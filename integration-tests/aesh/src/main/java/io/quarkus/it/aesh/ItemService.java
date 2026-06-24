package io.quarkus.it.aesh;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ItemService {

    @Inject
    EntityManager em;

    @Transactional
    public void add(String name) {
        em.persist(new Item(name));
    }

    @Transactional
    public List<Item> listAll() {
        return em.createQuery("SELECT i FROM Item i ORDER BY i.name", Item.class)
                .getResultList();
    }
}
