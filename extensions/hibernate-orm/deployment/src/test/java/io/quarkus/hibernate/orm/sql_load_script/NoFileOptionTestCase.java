package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Formatter;
import java.util.logging.Level;

import org.hamcrest.Matchers;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorSupport;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class NoFileOptionTestCase {
    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-no-file-option-test.properties", "application.properties")
                    .addAsResource("import.sql"))
            .setLogRecordPredicate(record -> HibernateProcessorSupport.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LOG_FORMATTER::formatMessage)
                    .containsExactly("Persistence unit '<default>' sets the deprecated configuration property"
                            + " 'quarkus.hibernate-orm.sql-load-script' to 'no-file'."
                            + " This is only needed as long as the file 'import.sql' exists in the classpath:"
                            + " consider moving its statements to 'data.sql' if they load data"
                            + " and/or to 'schema.sql' if they complete the schema."));

    @Test
    public void testSqlLoadScriptFileAbsentTest() {
        String name = "no entity";
        //despite the presence of import.sql, the file is not processed
        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is(name));
    }
}
