package io.quarkus.hibernate.reactive.panache.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.CreationException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.test.MyEntity;
import io.quarkus.hibernate.reactive.panache.test.MyEntityRepository;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;

public class ConfigActiveFalseRepositoryTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, MyEntityRepository.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.active", "false");

    @Inject
    MyEntityRepository repo;

    @Test
    @RunOnVertxContext
    public void test() {
        // The bean is always available to be injected during static init
        // since we don't know whether ORM will be active at runtime.
        // So the bean cannot be null.
        assertThat(repo).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> repo.findById(0L))
                .isInstanceOf(CreationException.class)
                .hasMessageContainingAll(
                        "Cannot retrieve the Mutiny.SessionFactory for persistence unit <default>",
                        "Hibernate Reactive was deactivated through configuration properties");
    }
}
