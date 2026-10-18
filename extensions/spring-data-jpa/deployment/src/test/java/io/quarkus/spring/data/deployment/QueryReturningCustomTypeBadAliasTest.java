package io.quarkus.spring.data.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class QueryReturningCustomTypeBadAliasTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("import_books.sql", "import.sql")
                    .addClasses(Book.class, BookRepositoryBadAlias.class))
            .withConfigurationResource("application.properties")
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void testBadAliases() {
        // an exception should be thrown
    }

}
