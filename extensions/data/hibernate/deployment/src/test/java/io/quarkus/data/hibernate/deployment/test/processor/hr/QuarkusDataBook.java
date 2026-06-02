/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor.hr;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.WithId;
import io.smallrye.mutiny.Uni;

@Entity
public class QuarkusDataBook extends WithId.AutoLong implements ManagedEntity.Reactive {
    public @NaturalId String isbn;
    public @NaturalId String title;
    public @NaturalId String author;
    public String text;
    public int pages;

    public interface Queries {
        @Find
        Uni<List<QuarkusDataBook>> findBook(String isbn);

        @HQL("WHERE isbn = :isbn")
        Uni<List<QuarkusDataBook>> hqlBook(String isbn);
    }
}
