package com.example;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory( "templatePU" );

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        Person person = new Person( 1L, "Bob", new Address( 1L, "Main Street" ) );
        entityManager.persist( person );
        entityManager.getTransaction().commit();
        entityManager.close();

        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        Person bob = entityManager.find( Person.class, 1L );

        assert bob != null;
        assert bob.getName().equals( "Bob" );
        assert bob.getAddress().getName().equals( "Main Street" );

        entityManager.getTransaction().commit();
        entityManager.close();

        entityManagerFactory.close();
    }
}
