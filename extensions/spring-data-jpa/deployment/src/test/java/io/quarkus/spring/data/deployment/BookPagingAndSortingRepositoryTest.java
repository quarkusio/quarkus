package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.quarkus.test.QuarkusUnitTest;

public class BookPagingAndSortingRepositoryTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("import_hp_books.sql", "import.sql")
                    .addClasses(Book.class, BookListPagingAndSortingRepository.class))
            .withConfigurationResource("application.properties");

    @Inject
    BookListPagingAndSortingRepository repo;

    @Test
    //    @Order(1)
    @Transactional
    public void shouldReturnFirstPageOfTwoBooks() {
        Pageable pageRequest = PageRequest.of(0, 2);
        Page<Book> result = repo.findAll(pageRequest);

        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(Book::getBid)).containsExactly(1, 2);
    }

    @Test
    @Transactional
    public void shouldReturnSecondPageOfSizeTwoBooks() {
        Pageable pageRequest = PageRequest.of(1, 2);
        Page<Book> result = repo.findAll(pageRequest);

        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(Book::getBid)).containsExactly(3, 4);
    }

    @Test
    @Transactional
    public void shouldReturnLastPage() {
        Pageable pageRequest = PageRequest.of(2, 2);
        Page<Book> result = repo.findAll(pageRequest);

        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(Book::getBid)).containsExactly(5, 6);
    }

    @Test
    @Transactional
    void shouldReturnSortedByNameAscAndPagedResult() {
        Pageable pageRequest = PageRequest.of(0, 3, Sort.by("name"));

        Page<Book> result = repo.findAll(pageRequest);
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(3);
        assertThat(result.stream().map(Book::getBid)).containsExactly(2, 7, 4);

    }

    @Test
    @Transactional
    void shouldReturnSortedByNameDescAndPagedResult() {
        Pageable pageRequest = PageRequest.of(0, 5, Sort.by("name").descending());

        Page<Book> result = repo.findAll(pageRequest);
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(5);
        assertThat(result.stream().map(Book::getBid)).containsExactly(3, 1, 5, 6, 4);

    }

    @Test
    @Transactional
    void shouldReturnAllBooksSortedByNameDescResult() {
        List<Book> result = repo.findAll(Sort.by("name").descending());
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(7);
        assertThat(result.stream().map(Book::getBid)).containsExactly(3, 1, 5, 6, 4, 7, 2);

    }

    @Test
    @Transactional
    void shouldReturnAllBooksSortedByNameAscResult() {
        List<Book> result = repo.findAll(Sort.by("name"));
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(7);
        assertThat(result.stream().map(Book::getBid)).containsExactly(2, 7, 4, 6, 5, 1, 3);

    }

}
