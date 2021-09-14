package com.example.grpc.hibernate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@ApplicationScoped
public class ItemDao {

    @Inject
    EntityManager entityManager;

    @Transactional
    public void add(Item newItem) {
        entityManager.persist(newItem);
    }
}
