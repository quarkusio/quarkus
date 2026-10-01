package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A file set explicitly as data init script is not picked up by default as schema init script as well:
 * with {@code data-management.init-script=import.sql}, import.sql is only executed once.
 */
public class ImportSqlAsDataInitScriptTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("import.sql"))
            .withConfiguration("""
                    quarkus.hibernate-orm.data-management.init-script=import.sql
                    quarkus.hibernate-orm.schema-management.halt-on-error=true
                    """);

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void importSqlExecutedOnce() {
        // A second execution would have failed on the primary key, and halt-on-error would have failed startup
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, 1L));
        assertThat(entity).isNotNull()
                .extracting(MyEntity::getName).isEqualTo("default sql load script entity");
    }
}
