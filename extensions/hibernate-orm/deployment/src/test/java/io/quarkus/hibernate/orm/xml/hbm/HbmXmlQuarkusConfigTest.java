package io.quarkus.hibernate.orm.xml.hbm;

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
 * Test that assigning an hbm.xml mapping file explicitly works as expected
 * when configuring the persistence unit through Quarkus' application.properties.
 */
public class HbmXmlQuarkusConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(SmokeTestUtils.class)
                    .addClass(SchemaUtil.class)
                    .addClass(NonAnnotatedEntity.class)
                    .addAsResource("application-mapping-files-my-hbm-xml.properties", "application.properties")
                    .addAsResource("META-INF/hbm-simple.xml", "my-hbm.xml"));

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
