package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class BothImportAndDataSqlTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("users1.sql", "data.sql")
                    .addAsResource("users2.sql", "import.sql")
                    .addClasses(User.class, LoginEvent.class, UserRepository.class))
            .withConfigurationResource("application.properties");

    @Inject
    UserRepository repo;

    @Test
    @Transactional
    public void test() {
        assertThat(repo.count()).isEqualTo(2);
    }
}
