package io.quarkus.hibernate.orm.xml.hbm;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class HbmXmlComponentTest {
    @RegisterExtension
    final static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(SchemaUtil.class)
                    .addClass(NonAnnotatedComponentUsingEntity.class)
                    .addClass(NonAnnotatedComponent.class)
                    .addAsResource("META-INF/hbm-component.xml", "my-hbm.xml"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping-files", "my-hbm.xml");

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void hbmXmlTakenIntoAccount() {
        assertThat(SchemaUtil.getColumnNames(entityManagerFactory, NonAnnotatedComponentUsingEntity.class))
                .contains("thename");
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager,
                NonAnnotatedComponentUsingEntity.class, NonAnnotatedComponentUsingEntity::new,
                NonAnnotatedComponentUsingEntity::getId,
                (e, name) -> e.getValue().setName(name), e -> e.getValue().getName());
    }

}
