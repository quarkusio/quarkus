package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import io.quarkus.test.QuarkusUnitTest;

public class CustomQueryMissingNamedParamTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("import_books.sql", "import.sql")
                    .addClasses(Book.class, BookRepositoryNamedParameterWrongName.class))
            .withConfigurationResource("application.properties")
            .assertException(e -> assertThat(e).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BookRepositoryNamedParameterWrongName is missing the named parameters [name], "
                            + "provided are [wrong]"));

    @Test
    public void testNamedParameters() {
        // an exception should be thrown
    }

    public interface BookRepositoryNamedParameterWrongName extends Repository<Book, Integer> {
        // wrong @Param annotation
        @Query(value = "FROM Book b WHERE b.name = :name")
        List<Book> queryBookByNameWrongAnnotation(@Param("wrong") String name);
    }
}
