/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.deployment.test.processor.orm;

import java.util.List;

import jakarta.data.repository.Delete;
import jakarta.persistence.Entity;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;

@Entity
public class Panache2Book extends PanacheEntity {
    public @NaturalId String isbn;
    public @NaturalId String title;
    public @NaturalId String author;
    public String text;
    public int pages;

    public interface Queries {
        @Find
        List<Panache2Book> findBook(String isbn);

        @HQL("WHERE isbn = :isbn")
        List<Panache2Book> hqlBook(String isbn);
    }

    public interface JDQueries {
        // This should work without @Repository
        @jakarta.data.repository.Find
        List<Panache2Book> findBook(String isbn);

        // should pick up the primary entity from the outer entity
        @Delete
        long deleteByTitle(String title);
    }

    // this should work just because we're extending a panache repo, no member required
    public interface MyRepo extends PanacheRepository<Panache2Book> {
    }

    public interface StatelessRepo extends PanacheRepository.Stateless<Long, Panache2Book> {
        // should pick up the primary entity from the outer entity
        @Delete
        long deleteByTitle(String title);
    }
}
