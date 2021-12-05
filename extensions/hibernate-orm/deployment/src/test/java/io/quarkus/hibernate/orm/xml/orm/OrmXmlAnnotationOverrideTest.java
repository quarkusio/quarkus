package io.quarkus.hibernate.orm.xml.orm;

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

/**
 * Test that assigning an orm.xml mapping file explicitly to override annotations
 * works as expected.
 */
public class OrmXmlAnnotationOverrideTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(SchemaUtil.class)
                    .addClass(AnnotatedEntity.class)
                    .addAsResource("application-mapping-files-my-orm-xml.properties", "application.properties")
                    .addAsResource("META-INF/orm-override.xml", "my-orm.xml"));

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void ormXmlTakenIntoAccount() {
        assertThat(SchemaUtil.getColumnNames(entityManagerFactory, AnnotatedEntity.class))
                .contains("thename")
                .doesNotContain("name");
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager,
                AnnotatedEntity.class, AnnotatedEntity::new,
                AnnotatedEntity::getId, AnnotatedEntity::setName, AnnotatedEntity::getName);
    }

}
