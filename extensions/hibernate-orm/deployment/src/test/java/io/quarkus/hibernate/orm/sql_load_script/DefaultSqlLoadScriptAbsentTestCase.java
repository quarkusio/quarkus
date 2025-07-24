package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultSqlLoadScriptAbsentTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            // In particular, we don't want Hibernate ORM to log
            // "Specified schema generation script file [import.sql] did not exist for reading"
            // when "import.sql" is just the Quarkus default.
            .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage).isEmpty());

    @Test
    public void testImportSqlLoadScriptTest() {
        // No startup failure, so we're already good.
    }
}
