package io.quarkus.it.spring.data.jpa;

import java.util.List;

import org.springframework.data.repository.Repository;

/**
 * Demonstrates the ability to add methods defined in the various Spring Data Repository interfaces
 * without actually having to extend those interfaces
 */
public interface BookRepository extends Repository<Book, Integer> {

    Book save(Book book);

    List<Book> findAll();

    boolean existsById(Integer id);

    boolean existsBookByPublicationYearBetween(Integer start, Integer end);
}
