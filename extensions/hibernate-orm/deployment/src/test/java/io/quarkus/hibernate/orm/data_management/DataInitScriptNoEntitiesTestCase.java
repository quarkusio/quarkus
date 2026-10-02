package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that data.sql is executed even when no @Entity classes are present.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/53413">quarkusio/quarkus#53413</a>
 */
public class DataInitScriptNoEntitiesTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("import-no-entities.sql", "data.sql"));

    @Inject
    StatelessSession statelessSession;

    @Test
    @Transactional
    public void testDataInitScriptExecutedWithoutEntities() {
        // import-no-entities.sql
        String name = statelessSession
                .createNativeQuery("SELECT name FROM test_data WHERE id = 1", String.class)
                .getSingleResult();
        assertThat(name).isEqualTo("imported without entities");
    }
}
