/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.deployment.test.processor.hr;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.WithId;
import io.smallrye.mutiny.Uni;

@Entity
public class Panache2Book extends WithId.AutoLong implements PanacheEntity.Reactive {
    public @NaturalId String isbn;
    public @NaturalId String title;
    public @NaturalId String author;
    public String text;
    public int pages;

    public interface Queries {
        @Find
        Uni<List<Panache2Book>> findBook(String isbn);

        @HQL("WHERE isbn = :isbn")
        Uni<List<Panache2Book>> hqlBook(String isbn);
    }
}
