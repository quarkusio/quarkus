package io.quarkus.hibernate.reactive.panache.test.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.CreationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.test.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;

public class ConfigActiveFalseActiveRecordTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.active", "false");

    @Test
    @RunOnVertxContext
    public void test() {
        assertThatThrownBy(() -> MyEntity.findById(0L))
                .isInstanceOf(CreationException.class)
                .hasMessageContainingAll(
                        "Cannot retrieve the Mutiny.SessionFactory for persistence unit <default>",
                        "Hibernate Reactive was deactivated through configuration properties");
    }
}
