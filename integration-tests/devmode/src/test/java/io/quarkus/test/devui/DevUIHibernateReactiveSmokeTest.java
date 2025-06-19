package io.quarkus.test.devui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class DevUIHibernateReactiveSmokeTest extends AbstractDevUIHibernateOrmTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource(new StringAsset("INSERT INTO MyEntity(id, field) VALUES(1, 'entity_1');"), "import.sql")
                    .addAsResource(new StringAsset("""
                            quarkus.datasource.jdbc=false
                            quarkus.datasource.db-kind=postgresql
                            quarkus.datasource.username=hibernate_orm_test
                            quarkus.datasource.password=hibernate_orm_test
                            quarkus.datasource.reactive.url=vertx-reactive:postgresql://localhost:5431/hibernate_orm_test
                            quarkus.hibernate-orm.blocking=false
                            """),
                            "application.properties"));

    public DevUIHibernateReactiveSmokeTest() {
        super("default-reactive", "MyEntity", "io.quarkus.test.devui.MyEntity", null, true);
    }

    @Test
    @Override
    public void testExecuteHQL() {
        // HQL execution is not supported in reactive mode, so we skip this test
    }
}
