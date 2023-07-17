package io.quarkus.hibernate.orm.xml.hbm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class HbmXmlFilterDefTest {
    @RegisterExtension
    final static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(NonAnnotatedEntity.class)
                    .addAsResource("META-INF/hbm-filterdef.xml", "my-hbm.xml"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping-files", "my-hbm.xml")
            .overrideConfigKey("quarkus.hibernate-orm.log.sql", "true");

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    @Test
    @Transactional
    public void hbmXmlTakenIntoAccount() {
        assertThat(sessionFactory.getDefinedFilterNames())
                .contains("idFilter");
    }

    @Test
    @Transactional
    public void smokeTest() {
        NonAnnotatedEntity firstEntity = new NonAnnotatedEntity("first");
        session.persist(firstEntity);
        NonAnnotatedEntity secondEntity = new NonAnnotatedEntity("second");
        session.persist(secondEntity);
        session.flush();

        assertThat(session.createQuery("select e from " + NonAnnotatedEntity.class.getName() + " e",
                NonAnnotatedEntity.class).list())
                .hasSize(2);
        session.enableFilter("idFilter")
                .setParameterList("ids", Collections.singletonList(firstEntity.getId()));
        assertThat(session.createQuery("select e from " + NonAnnotatedEntity.class.getName() + " e",
                NonAnnotatedEntity.class).list())
                .hasSize(1);
    }

}
