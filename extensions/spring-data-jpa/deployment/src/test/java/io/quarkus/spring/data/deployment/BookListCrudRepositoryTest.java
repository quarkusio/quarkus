package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookListCrudRepositoryTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("import_books.sql", "import.sql")
                    .addClasses(Book.class, BookListCrudRepository.class))
            .withConfigurationResource("application.properties");

    @Inject
    BookListCrudRepository repo;

    @Test
    @Order(1)
    @Transactional
    public void shouldListAllBooks() {
        List<Book> all = repo.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all).hasSize(3);
        assertThat(all.stream().map(Book::getName)).containsExactlyInAnyOrder("Talking to Strangers", "The Ascent of Money",
                "A Short History of Everything");
    }

    @Test
    @Order(2)
    @Transactional
    public void shouldListBooksWithIds() {
        List<Integer> ids = Arrays.asList(1, 2);
        List<Book> all = repo.findAllById(ids);
        assertThat(all).isNotEmpty();
        assertThat(all).hasSize(2);
        assertThat(all.stream().map(Book::getName)).containsExactlyInAnyOrder("Talking to Strangers", "The Ascent of Money");
    }

    @Test
    @Order(3)
    @Transactional
    public void shouldSaveBooks() {
        Book harryPotterAndTheChamberOfSecrets = populateBook(4, "Harry Potter and the Chamber of Secrets");
        Book harryPotterAndThePrisonerOfAzkaban = populateBook(5, "Harry Potter and the Prisoner of Azkaban");
        Book harryPotterAndTheGlobetOfFire = populateBook(6, "Harry Potter and the Globet of Fire");
        List<Book> books = Arrays.asList(harryPotterAndTheChamberOfSecrets, harryPotterAndThePrisonerOfAzkaban,
                harryPotterAndTheGlobetOfFire);
        List<Book> all = repo.saveAll(books);
        assertThat(all).isNotEmpty();
        assertThat(all).hasSize(3);
        assertThat(all.stream().map(Book::getName)).containsExactlyInAnyOrder("Harry Potter and the Chamber of Secrets",
                "Harry Potter and the Prisoner of Azkaban", "Harry Potter and the Globet of Fire");
    }

    @Test
    @Transactional
    public void optionalWithNonExisting() {
        assertThat(repo.findFirstByNameOrderByBid("foobar")).isEmpty();
    }

    private Book populateBook(Integer id, String title) {
        Book book = new Book();
        book.setBid(id);
        book.setName(title);
        return book;
    }

}
