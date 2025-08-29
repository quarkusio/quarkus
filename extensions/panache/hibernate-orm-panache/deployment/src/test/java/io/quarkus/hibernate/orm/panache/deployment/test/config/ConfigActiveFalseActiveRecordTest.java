package io.quarkus.hibernate.orm.panache.deployment.test.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.control.ActivateRequestContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.deployment.test.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseActiveRecordTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.active", "false");

    @Test
    @ActivateRequestContext
    public void test() {
        assertThatThrownBy(() -> MyEntity.findById(0L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll(
                        "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit <default>",
                        "Hibernate ORM was deactivated through configuration properties");
    }
}
