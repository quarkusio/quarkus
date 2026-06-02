/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor.orm;

import java.util.List;

import jakarta.data.repository.Delete;
import jakarta.persistence.Entity;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.data.hibernate.RecordRepository;

@Entity
public class QuarkusDataBook extends ManagedEntity.AutoLong {
    public @NaturalId String isbn;
    public @NaturalId String title;
    public @NaturalId String author;
    public String text;
    public int pages;

    public interface Queries {
        @Find
        List<QuarkusDataBook> findBook(String isbn);

        @HQL("WHERE isbn = :isbn")
        List<QuarkusDataBook> hqlBook(String isbn);
    }

    public interface JDQueries {
        // This should work without @Repository
        @jakarta.data.repository.Find
        List<QuarkusDataBook> findBook(String isbn);

        // should pick up the primary entity from the outer entity
        @Delete
        long deleteByTitle(String title);
    }

    // this should work just because we're extending a panache repo, no member required
    public interface MyRepo extends ManagedRepository.AutoLong<QuarkusDataBook> {
    }

    public interface StatelessRepo extends RecordRepository<Long, QuarkusDataBook> {
        // should pick up the primary entity from the outer entity
        @Delete
        long deleteByTitle(String title);
    }
}
