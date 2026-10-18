package io.quarkus.spring.data.deployment;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface BookRepositoryBadAlias extends Repository<Book, Integer> {

    // issue 6205: field name does not match getter name
    @Query(value = "SELECT publicationYear as year, COUNT(*) as count FROM Book GROUP BY publicationYear")
    List<BookCountByYear> findAllByPublicationYear();

    interface BookCountByYear {
        int getPublicationYear();

        Long getCount();
    }
}
