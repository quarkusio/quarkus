package io.quarkus.hibernate.orm.log;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class LogSqlFormatSqlFalseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application-log-sql-format-sql-false.properties", "application.properties"));

    @Inject
    EntityManager em;

    @BeforeEach
    public void activateRequestContext() {
        Arc.container().requestContext().activate();
    }

    @Test
    public void testFormattedValue() {
        Map<String, Object> properties = em.getEntityManagerFactory().getProperties();
        assertNull(properties.get(AvailableSettings.FORMAT_SQL));
    }

    @AfterEach
    public void terminateRequestContext() {
        Arc.container().requestContext().terminate();
    }
}
