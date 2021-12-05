package io.quarkus.hibernate.orm.sql_load_script;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ImportMultipleSqlLoadScriptsFileAbsentTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setExpectedException(ConfigurationException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-import-multiple-load-scripts-test.properties", "application.properties")
                    .addAsResource("import-multiple-load-scripts-1.sql", "import-1.sql"));

    @Test
    public void testImportMultipleSqlLoadScriptsTest() {
        // should not be called, deployment exception should happen first:
        // it's illegal to have Hibernate sql-load-script configuration property set
        // to an absent file
        Assertions.fail();
    }
}
