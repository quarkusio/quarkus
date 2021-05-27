package io.quarkus.hibernate.orm.use_jdbc_metadata_defaults;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class UseJdbcMetadataDefaultsFalseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEntity.class)
                    .addAsResource("application-use-jdbc-metadata-defaults-false-value.properties", "application.properties"));

    @Inject
    EntityManager em;

    @ActivateRequestContext
    @Test
    public void testFalseValue() {
        Map<String, Object> properties = em.getEntityManagerFactory().getProperties();
        assertEquals("false", properties.get("hibernate.temp.use_jdbc_metadata_defaults"));
    }
}
