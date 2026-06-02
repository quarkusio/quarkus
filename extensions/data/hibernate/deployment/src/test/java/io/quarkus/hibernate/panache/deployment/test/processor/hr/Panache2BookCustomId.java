/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.deployment.test.processor.hr;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveRepositoryBase;
import io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveRepositoryBase;
import io.smallrye.mutiny.Uni;

@Entity
public class Panache2BookCustomId implements PanacheEntity.Reactive {
    public @NaturalId String isbn;
    public @NaturalId String title;
    public @NaturalId String author;
    public String text;
    public int pages;
    public @Id String myid;

    public interface DoesThisWork {
        @Find
        Uni<List<Panache2BookCustomId>> findBook(String isbn);
    }

    public interface ManagedQueries extends PanacheManagedReactiveRepositoryBase<Panache2BookCustomId, String> {
        @Find
        Uni<List<Panache2BookCustomId>> findBook(String isbn);
    }

    public interface StatelessQueries extends PanacheStatelessReactiveRepositoryBase<Panache2BookCustomId, String> {
        @Find
        Uni<List<Panache2BookCustomId>> findBook(String isbn);
    }
}
