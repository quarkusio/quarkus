package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class FileNotFoundSqlLoadScriptTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.sql-load-script", "file-that-does-not-exist.sql")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll("Unable to find file referenced in '"
                            + "quarkus.hibernate-orm.sql-load-script=file-that-does-not-exist.sql'. Remove property or add file to your path."));

    @Test
    public void testSqlLoadScriptFileAbsentTest() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
