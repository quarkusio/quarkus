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

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.data.hibernate.RecordRepository;

@Entity
public class QuarkusDataBookCustomId implements ManagedEntity.CustomId {
    public @NaturalId String isbn;
    public @NaturalId String title;
    public @NaturalId String author;
    public String text;
    public int pages;
    public @Id String myid;

    public interface DoesThisWork {
        @Find
        List<QuarkusDataBookCustomId> findBook(String isbn);
    }

    public interface ManagedQueries extends ManagedRepository.CustomId<QuarkusDataBookCustomId, String> {
        @Find
        List<QuarkusDataBookCustomId> findBook(String isbn);
    }

    public interface StatelessQueries extends RecordRepository.CustomId<QuarkusDataBookCustomId, String> {
        @Find
        List<QuarkusDataBookCustomId> findBook(String isbn);
    }
}
