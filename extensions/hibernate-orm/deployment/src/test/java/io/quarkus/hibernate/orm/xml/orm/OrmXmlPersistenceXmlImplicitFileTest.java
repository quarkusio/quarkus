package io.quarkus.hibernate.orm.xml.orm;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that the implicit mapping file META-INF/orm.xml is taken into account
 * for persistence units configured through persistence.xml,
 * even if it is not mentioned in the persistence.xml.
 */
public class OrmXmlPersistenceXmlImplicitFileTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(SmokeTestUtils.class)
                    .addClass(SchemaUtil.class)
                    .addClass(NonAnnotatedEntity.class)
                    .addAsResource("application-datasource-only.properties", "application.properties")
                    .addAsManifestResource("META-INF/persistence-mapping-file-implicit-orm-xml.xml", "persistence.xml")
                    // META-INF/orm.xml should be picked up even if it's not mentioned explicitly.
                    .addAsManifestResource("META-INF/orm-simple.xml", "orm.xml"));

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void ormXmlTakenIntoAccount() {
        assertThat(SchemaUtil.getColumnNames(entityManagerFactory, NonAnnotatedEntity.class))
                .contains("thename")
                .doesNotContain("name");
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager,
                NonAnnotatedEntity.class, NonAnnotatedEntity::new,
                NonAnnotatedEntity::getId, NonAnnotatedEntity::setName, NonAnnotatedEntity::getName);
    }

}
