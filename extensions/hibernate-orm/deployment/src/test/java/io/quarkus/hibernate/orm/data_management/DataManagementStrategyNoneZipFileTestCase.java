package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With the "none" data management strategy, a data init script packaged as a zip file
 * stays available to explicit {@link org.hibernate.relational.SchemaManager} calls after startup.
 */
public class DataManagementStrategyNoneZipFileTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class)
                    .addAsResource("load-script-test.zip"))
            .withConfiguration("""
                    quarkus.hibernate-orm.data-management.init-script=load-script-test.zip
                    """)
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "none");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void zipDataInitScriptAvailableToSchemaManager() {
        assertThat(entityName(3)).isNull();

        sessionFactory.getSchemaManager().populate();
        assertThat(entityName(3)).isEqualTo("other-load-script sql load script entity");
    }

    private String entityName(long id) {
        MyEntity entity = sessionFactory.fromTransaction(session -> session.find(MyEntity.class, id));
        return entity == null ? null : entity.getName();
    }
}
