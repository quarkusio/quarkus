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
 * Test that the <package> element is correctly interpreted in an orm.xml mapping file.
 */
public class OrmXmlPackageTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(SchemaUtil.class)
                    .addClass(NonAnnotatedEntity.class)
                    .addClass(OtherNonAnnotatedEntity.class)
                    .addAsResource("META-INF/orm-package.xml", "my-orm.xml"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping-files", "my-orm.xml");

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
        assertThat(SchemaUtil.getColumnNames(entityManagerFactory, OtherNonAnnotatedEntity.class))
                .contains("thename")
                .doesNotContain("name");
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager,
                NonAnnotatedEntity.class, NonAnnotatedEntity::new,
                NonAnnotatedEntity::getId, NonAnnotatedEntity::setName, NonAnnotatedEntity::getName);
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager,
                OtherNonAnnotatedEntity.class, OtherNonAnnotatedEntity::new,
                OtherNonAnnotatedEntity::getId, OtherNonAnnotatedEntity::setName, OtherNonAnnotatedEntity::getName);
    }

}
