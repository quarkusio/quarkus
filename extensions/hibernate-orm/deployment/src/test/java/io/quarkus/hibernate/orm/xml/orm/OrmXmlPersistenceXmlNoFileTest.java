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
 * Test that the implicit mapping file META-INF/orm.xml is ignored
 * for persistence units configured through Quarkus' application.properties.
 */
public class OrmXmlPersistenceXmlNoFileTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(SmokeTestUtils.class)
                    .addClass(SchemaUtil.class)
                    .addClass(AnnotatedEntity.class)
                    .addAsResource("application-datasource-only.properties", "application.properties")
                    .addAsManifestResource("META-INF/persistence-mapping-file-no-file.xml", "persistence.xml")
                    // For a Quarkus persistence unit,
                    // we will ignore the default META-INF/orm.xml unless it's specified explicitly.
                    // That's to reduce the amount of magic needed,
                    // and to make sure users can still build an application when they depend on libraries
                    // that contain undesirable orm.xml files (potentially multiple ones, which would fail).
                    .addAsManifestResource("META-INF/orm-invalid.xml", "orm.xml"));

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void ormXmlIgnored() {
        assertThat(SchemaUtil.getColumnNames(entityManagerFactory, AnnotatedEntity.class))
                .contains("name")
                .doesNotContain("someothername");
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager,
                AnnotatedEntity.class, AnnotatedEntity::new,
                AnnotatedEntity::getId, AnnotatedEntity::setName, AnnotatedEntity::getName);
    }

}
