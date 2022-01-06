package io.quarkus.hibernate.orm.xml.hbm;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class HbmXmlComplexFileTest {
    @RegisterExtension
    final static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(SchemaUtil.class)
                    .addClass(NonAnnotatedComplexEntity.class)
                    .addClass(NonAnnotatedComponentEntity.class)
                    .addAsResource("application-mapping-files-my-complex-hbm-xml.properties", "application.properties")
                    .addAsResource("META-INF/hbm-complex.xml", "my-complex-hbm.xml"));

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void ormXmlTakenIntoAccount() {
        assertThat(SchemaUtil.getColumnNames(entityManagerFactory, NonAnnotatedComplexEntity.class))
                .contains("thename")
                .doesNotContain("name");
        assertThat(SchemaUtil.getColumnNames(entityManagerFactory, NonAnnotatedComplexEntity.class))
                .contains("value");
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager,
                NonAnnotatedComplexEntity.class, NonAnnotatedComplexEntity::new,
                NonAnnotatedComplexEntity::getId, NonAnnotatedComplexEntity::setName, NonAnnotatedComplexEntity::getName);
    }

}
