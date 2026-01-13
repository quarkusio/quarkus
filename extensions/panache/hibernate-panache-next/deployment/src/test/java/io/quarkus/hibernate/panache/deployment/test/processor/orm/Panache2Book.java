/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.deployment.test.processor.orm;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.panache.PanacheEntity;

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
}
