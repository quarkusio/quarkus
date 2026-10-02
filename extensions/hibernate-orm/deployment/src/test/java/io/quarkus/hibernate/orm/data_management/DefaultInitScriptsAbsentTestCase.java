package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

public class DefaultInitScriptsAbsentTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            // In particular, we don't want Hibernate ORM to log
            // "Specified schema generation script file [import.sql] did not exist for reading"
            // when "import.sql" or "data.sql" is just the Quarkus default.
            .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage).isEmpty());

    @Test
    public void testNoWarningWhenDefaultInitScriptsAreAbsent() {
        // No startup failure, so we're already good.
    }
}
