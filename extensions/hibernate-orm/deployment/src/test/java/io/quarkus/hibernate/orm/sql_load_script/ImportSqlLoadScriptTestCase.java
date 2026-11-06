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

public class ImportSqlLoadScriptTestCase {
    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-import-load-script-test.properties", "application.properties")
                    .addAsResource("import.sql"))
            .setLogRecordPredicate(record -> HibernateProcessorSupport.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LOG_FORMATTER::formatMessage)
                    .containsExactly("Persistence unit '<default>' uses the deprecated configuration property"
                            + " 'quarkus.hibernate-orm.sql-load-script'."
                            + " Use 'quarkus.hibernate-orm.data-management.init-script' to load data"
                            + " (executed regardless of how the schema is managed)"
                            + " or 'quarkus.hibernate-orm.schema-management.init-script' to complete the schema"
                            + " (executed only when Hibernate ORM creates the schema)."
                            + " Don't name these scripts 'import.sql':"
                            + " that file enables the deprecated behavior of 'quarkus.hibernate-orm.sql-load-script'."));

    @Test
    public void testImportSqlLoadScriptTest() {
        String name = "import.sql load script entity";
        RestAssured.when().get("/orm-sql-load-script/2").then().body(Matchers.is(name));
    }
}
