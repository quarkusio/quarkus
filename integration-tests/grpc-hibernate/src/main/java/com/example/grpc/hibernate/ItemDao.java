package com.example.grpc.hibernate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ItemDao {

    @Inject
    EntityManager entityManager;

    @Transactional
    public void add(Item newItem) {
        entityManager.persist(newItem);
    }
}
