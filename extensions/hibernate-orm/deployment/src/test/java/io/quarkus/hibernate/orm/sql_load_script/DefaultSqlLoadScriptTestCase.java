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

/**
 * An "import.sql" file enables the deprecated "sql-load-script" behavior, with a warning.
 */
public class DefaultSqlLoadScriptTestCase {
    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addClasses(SqlLoadScriptTestResource.class, MyEntity.class))
            .setLogRecordPredicate(record -> HibernateProcessorSupport.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LOG_FORMATTER::formatMessage)
                    .containsExactly("Persistence unit '<default>' uses the deprecated behavior of"
                            + " 'quarkus.hibernate-orm.sql-load-script', because the file 'import.sql' exists in the classpath."
                            + " Move its statements to 'data.sql' if they load data (executed regardless of how the schema is managed)"
                            + " and/or to 'schema.sql' if they complete the schema (executed only when Hibernate ORM creates the schema),"
                            + " then delete it."));

    @Test
    public void testDefaultSqlLoadScriptTest() {
        String name = "default sql load script entity";
        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is(name));
    }
}
