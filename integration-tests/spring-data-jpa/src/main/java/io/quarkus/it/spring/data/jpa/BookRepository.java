package io.quarkus.it.spring.data.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Demonstrates the ability to add methods defined in the various Spring Data Repository interfaces
 * without actually having to extend those interfaces
 */
public interface BookRepository extends Repository<Book, Integer> {

    Book save(Book book);

    List<Book> findAll();

    List<Book> findByName(String name);

    boolean existsByBid(Integer id);

    boolean existsBookByPublicationYearBetween(Integer start, Integer end);

    Optional<Book> findByPublicationYear(Integer year);

    // issue 6205
    @Query(value = "SELECT publicationYear as publicationYear, COUNT(*) as count FROM Book GROUP BY publicationYear")
    List<BookCountByYear> findAllByPublicationYear();

    // issue 6205
    @Query(value = "SELECT COUNT(*), publicationYear FROM Book GROUP BY publicationYear")
    List<BookCountByYear> findAllByPublicationYear2();

    interface BookCountByYear {
        int getPublicationYear();

        Long getCount();
    }
}
