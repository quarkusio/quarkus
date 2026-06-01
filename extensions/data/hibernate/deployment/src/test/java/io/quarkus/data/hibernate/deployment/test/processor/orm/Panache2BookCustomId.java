/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor.orm;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;

import io.quarkus.data.hibernate.PanacheEntity;
import io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingRepositoryBase;
import io.quarkus.data.hibernate.stateless.blocking.PanacheStatelessBlockingRepositoryBase;

@Entity
public class Panache2BookCustomId implements PanacheEntity.Managed {
    public @NaturalId String isbn;
    public @NaturalId String title;
    public @NaturalId String author;
    public String text;
    public int pages;
    public @Id String myid;

    public interface DoesThisWork {
        @Find
        List<Panache2BookCustomId> findBook(String isbn);
    }

    public interface ManagedQueries extends PanacheManagedBlockingRepositoryBase<Panache2BookCustomId, String> {
        @Find
        List<Panache2BookCustomId> findBook(String isbn);
    }

    public interface StatelessQueries extends PanacheStatelessBlockingRepositoryBase<Panache2BookCustomId, String> {
        @Find
        List<Panache2BookCustomId> findBook(String isbn);
    }
}
