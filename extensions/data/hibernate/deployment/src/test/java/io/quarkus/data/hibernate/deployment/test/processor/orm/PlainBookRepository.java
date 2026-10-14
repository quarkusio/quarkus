/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor.orm;

import java.util.List;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

/**
 * A plain interface, not a Quarkus Data repository and with no session getter: in a Quarkus ORM
 * environment the processor must pick a default (blocking) session and inject it.
 */
public interface PlainBookRepository {
    @Find
    List<QuarkusDataBook> findBook(String isbn);

    @HQL("FROM io.quarkus.data.hibernate.deployment.test.processor.orm.QuarkusDataBook WHERE isbn = :isbn")
    List<QuarkusDataBook> hqlBook(String isbn);
}
