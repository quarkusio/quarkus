package io.quarkus.it.panache;

import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Beer extends PanacheEntity {

    public String name;

    @Transactional
    public static void deleteAllAndPersist(Beer beer) {
        deleteAll();
        persist(beer);
    }

}
