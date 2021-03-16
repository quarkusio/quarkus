package io.quarkus.it.spring.data.rest;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface BooksRepository extends PagingAndSortingRepository<Book, Long> {
}
