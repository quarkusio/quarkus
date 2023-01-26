package io.quarkus.hibernate.orm.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.transaction.UserTransaction;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;

public class ApplicationScopedStatementInspectorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addClass(ApplicationStatementInspector.class))
            .withConfigurationResource("application.properties");

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    @Inject
    UserTransaction transaction;

    public void initData(@Observes StartupEvent event) throws Exception {
        transaction.begin();
        for (int i = 0; i < 3; i++) {
            MyEntity entity = new MyEntity(i);
            session.persist(entity);
        }
        transaction.commit();
    }

    @BeforeEach
    public void clearStatementInspector() {
        ApplicationStatementInspector.statements.clear();
    }

    @Test
    public void testStatementInspectorIsLoaded() throws Exception {
        transaction.begin();
        session.find(MyEntity.class, 0);
        transaction.commit();
        assertThat(ApplicationStatementInspector.statements).hasSize(1);
    }

    @Entity(name = "myentity")
    @Table
    public static class MyEntity {

        @Id
        public Integer id;

        public MyEntity() {
        }

        public MyEntity(int id) {
            this.id = id;
        }
    }

    @PersistenceUnitExtension // @ApplicationScoped is the default
    public static class ApplicationStatementInspector implements StatementInspector {

        static List<String> statements = new ArrayList<>();

        @Override
        public String inspect(String sql) {
            statements.add(sql);
            return sql;
        }
    }
}
