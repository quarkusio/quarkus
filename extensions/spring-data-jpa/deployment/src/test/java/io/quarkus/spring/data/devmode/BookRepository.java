package io.quarkus.spring.data.devmode;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface BookRepository extends Repository<Book, Integer> {

    List<Book> findAll();

    // <placeholder>
}
