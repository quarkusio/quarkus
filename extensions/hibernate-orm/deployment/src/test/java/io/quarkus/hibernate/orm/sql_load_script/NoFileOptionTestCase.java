package io.quarkus.hibernate.orm.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorSupport;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class NoFileOptionTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-no-file-option-test.properties", "application.properties")
                    .addAsResource("import.sql"))
            .setLogRecordPredicate(record -> HibernateProcessorSupport.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            // Setting the deprecated property to 'no-file' is fine: removing the property later won't change anything
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Test
    public void testSqlLoadScriptFileAbsentTest() {
        String name = "no entity";
        //despite the presence of import.sql, the file is not processed
        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is(name));
    }
}
