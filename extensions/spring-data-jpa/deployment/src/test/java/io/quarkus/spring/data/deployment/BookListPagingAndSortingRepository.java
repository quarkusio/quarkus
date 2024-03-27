package io.quarkus.spring.data.deployment;

import org.springframework.data.repository.ListPagingAndSortingRepository;

public interface BookListPagingAndSortingRepository extends ListPagingAndSortingRepository<Book, Integer> {
}
