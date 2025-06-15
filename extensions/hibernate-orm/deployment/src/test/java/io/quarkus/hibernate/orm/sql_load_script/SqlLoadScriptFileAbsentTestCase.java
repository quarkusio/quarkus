package io.quarkus.hibernate.orm.sql_load_script;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class SqlLoadScriptFileAbsentTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().setExpectedException(ConfigurationException.class)
            .withApplicationRoot((jar) -> jar.addClasses(MyEntity.class)
                    .addAsResource("application-other-load-script-test.properties", "application.properties"));

    @Test
    public void testSqlLoadScriptFileAbsentTest() {
        // should not be called, deployment exception should happen first:
        // it's illegal to have Hibernate sql-load-script configuration property set
        // to an absent file
        Assertions.fail();
    }
}
