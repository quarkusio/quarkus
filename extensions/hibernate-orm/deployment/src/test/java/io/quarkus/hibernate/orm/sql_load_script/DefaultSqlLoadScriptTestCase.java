package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DefaultSqlLoadScriptTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addClasses(SqlLoadScriptTestResource.class, MyEntity.class));

    @Test
    public void testDefaultSqlLoadScriptTest() {
        String name = "default sql load script entity";
        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is(name));
    }
}
