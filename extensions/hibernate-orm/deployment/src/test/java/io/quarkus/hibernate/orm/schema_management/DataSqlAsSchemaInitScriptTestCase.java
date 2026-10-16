package io.quarkus.hibernate.orm.schema_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A file set explicitly as schema init script is not picked up by default as data init script as well:
 * with {@code schema-management.init-script=data.sql}, data.sql is only executed once.
 */
public class DataSqlAsSchemaInitScriptTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("data.sql"))
            .withConfiguration("""
                    quarkus.hibernate-orm.schema-management.init-script=data.sql
                    quarkus.hibernate-orm.schema-management.halt-on-error=true
                    """);

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void dataSqlExecutedOnce() {
        // A second execution would have failed on the primary key, and halt-on-error would have failed startup
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, 10L));
        assertThat(entity).isNotNull()
                .extracting(MyEntity::getName).isEqualTo("data.sql data init script entity");
    }
}
