package io.quarkus.hibernate.orm.panache.deployment.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.deployment.test.inheritance.ChildEntity;
import io.quarkus.hibernate.orm.panache.deployment.test.inheritance.MappedParent;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class InheritanceNoFieldsTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MappedParent.class, ChildEntity.class, InheritanceResource.class)
                    .addAsResource(new StringAsset("INSERT INTO ChildEntity(id, name) VALUES(1, 'my name');\n"), "import.sql")
                    .addAsResource("application-test.properties",
                            "application.properties"));

    @Test
    public void testInheritanceNoFields() {

        RestAssured.when().get("/entity/1").then().body(Matchers.is("my name"));
    }

}
