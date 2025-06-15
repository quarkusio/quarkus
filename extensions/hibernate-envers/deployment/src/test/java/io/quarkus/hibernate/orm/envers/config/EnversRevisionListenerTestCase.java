package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.hibernate.orm.envers.MyListenerlessRevisionEntity;
import io.quarkus.hibernate.orm.envers.MyListenerlessRevisionListener;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversRevisionListenerTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MyAuditedEntity.class, MyListenerlessRevisionEntity.class, MyListenerlessRevisionListener.class,
                    EnversTestRevisionListenerResource.class, AbstractEnversResource.class)
            .addAsResource("application-with-revision-listener.properties", "application.properties"));

    @Test
    public void testRevisionListener() {
        RestAssured.when().get("/envers-revision-listener").then().body(is("OK"));
    }
}
