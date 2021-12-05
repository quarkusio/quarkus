package io.quarkus.hibernate.orm.ignore_explicit_for_joined;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class IgnoreExplicitForJoinedTrueValueTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("application-discriminator-ignore-explicit-for-joined-true-value.properties",
                            "application.properties"));

    @Inject
    EntityManager em;

    @ActivateRequestContext
    @Test
    public void testTrueValue() {
        Map<String, Object> properties = em.getEntityManagerFactory().getProperties();
        assertEquals("true", properties.get("hibernate.discriminator.ignore_explicit_for_joined"));
    }
}
