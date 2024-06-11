package io.quarkus.spring.data.deployment;

import org.springframework.data.repository.ListCrudRepository;

public interface BookListCrudRepository extends ListCrudRepository<Book, Integer> {
}
