package io.quarkus.spring.data.deployment;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;

public interface BookListCrudRepository extends ListCrudRepository<Book, Integer> {

    Optional<Book> findFirstByNameOrderByBid(String name);
}
