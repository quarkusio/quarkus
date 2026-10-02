/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.hibernate.deployment.test.processor.orm;

import java.util.List;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface BookJakartaDataRepository extends CrudRepository<QuarkusDataBook, Long> {
    @Find
    List<QuarkusDataBook> findBook(String isbn);

    @Query("FROM io.quarkus.data.hibernate.deployment.test.processor.orm.QuarkusDataBook WHERE isbn = :isbn")
    List<QuarkusDataBook> hqlBook(String isbn);
}
