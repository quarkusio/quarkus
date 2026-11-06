package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.shared.SharedEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.User;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An "import.sql" file is picked up by every persistence unit,
 * so a persistence unit that uses the SQL init scripts next to it is ambiguous,
 * even if other persistence units rely on that file.
 */
public class MultiplePersistenceUnitsImportSqlAndDataInitScriptTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(Plane.class.getPackage().getName())
                    .addPackage(SharedEntity.class.getPackage().getName())
                    .addPackage(User.class.getPackage().getName())
                    .addAsResource("application-multiple-persistence-units-annotations.properties",
                            "application.properties")
                    .addAsResource("data-plane.sql", "import.sql")
                    .addAsResource("data-plane.sql"))
            .overrideConfigKey("quarkus.hibernate-orm.\"inventory\".data-management.init-script", "data-plane.sql")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Persistence unit 'inventory' would use both the deprecated SQL load script behavior"
                                    + " (the file 'import.sql' exists in the classpath)",
                            "('quarkus.hibernate-orm.\"inventory\".data-management.init-script' is set)",
                            "or set 'quarkus.hibernate-orm.\"inventory\".sql-load-script' to 'no-file'."));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
