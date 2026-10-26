package io.quarkus.hibernate.orm.schema_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Like data init scripts, schema init scripts can be packaged as zip files.
 */
public class ZipSchemaInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("load-script-test.zip"))
            .withConfiguration("""
                    quarkus.hibernate-orm.schema-management.init-script=load-script-test.zip
                    """);

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void zipSchemaInitScriptExecuted() {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, 3L));
        assertThat(entity).isNotNull()
                .extracting(MyEntity::getName).isEqualTo("other-load-script sql load script entity");
    }
}
