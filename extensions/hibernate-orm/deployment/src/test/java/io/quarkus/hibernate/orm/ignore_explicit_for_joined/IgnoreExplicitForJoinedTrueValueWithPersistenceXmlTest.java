package io.quarkus.hibernate.orm.ignore_explicit_for_joined;

import static org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.xml.persistence.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class IgnoreExplicitForJoinedTrueValueWithPersistenceXmlTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsManifestResource("META-INF/persistence-discriminator-ignore-explicit-for-joined-true-value.xml",
                            "persistence.xml")
                    .addAsResource("application-datasource-only.properties", "application.properties"));

    @Inject
    EntityManager em;

    @ActivateRequestContext
    @Test
    public void testTrueValue() {
        Map<String, Object> properties = em.getEntityManagerFactory().getProperties();

        // the PU is templatePU from the persistence.xml, not the default entity manager from application.properties
        assertEquals("templatePU", properties.get(PERSISTENCE_UNIT_NAME));
        assertEquals("true", properties.get("hibernate.discriminator.ignore_explicit_for_joined"));
    }
}
