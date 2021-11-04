package io.quarkus.hibernate.orm.sql_load_script;

import javax.persistence.PersistenceException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidMultilineSqlLoadScriptTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setExpectedException(PersistenceException.class)
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-invalid-multiline-test.properties", "application.properties")
                    .addAsResource("invalid-multiline.sql")
                    .addClasses(MyEntity.class));

    @Test
    public void testSqlLoadScriptFileAbsentTest() {
        // should not be called, deployment exception should happen first.
        // A multiline sql file with an sql statement not terminated by a semicolon should fail.
        Assertions.fail();
    }
}
