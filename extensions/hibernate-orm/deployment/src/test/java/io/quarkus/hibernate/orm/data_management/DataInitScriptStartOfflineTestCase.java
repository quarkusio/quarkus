package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When starting offline, the default data init script (data.sql) is simply not executed:
 * the application must start without connecting to the database.
 */
public class DataInitScriptStartOfflineTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("data.sql")
                    .addAsResource("application-start-offline.properties", "application.properties"));

    @Inject
    Session session;

    @Test
    public void applicationStarts() {
        assertThat(session).isNotNull();
    }
}
