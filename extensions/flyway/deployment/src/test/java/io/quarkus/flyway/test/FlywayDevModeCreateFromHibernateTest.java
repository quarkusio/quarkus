package io.quarkus.flyway.test;

import java.util.List;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Startup;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class FlywayDevModeCreateFromHibernateTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FlywayDevModeCreateFromHibernateTest.class, Endpoint.class, Fruit.class)
                    .addAsResource(new StringAsset(
                            "quarkus.flyway.locations=db/create"), "application.properties"));

    @Test
    public void testGenerateMigrationFromHibernate() {
        RestAssured.get("fruit").then().statusCode(200)
                .body("[0].name", CoreMatchers.is("Orange"));
        RestAssured.given().redirects().follow(false).formParam("datasource", "<default>")
                .post("/q/dev/io.quarkus.quarkus-flyway/create-initial-migration").then().statusCode(303);

        config.modifySourceFile(Fruit.class, s -> s.replace("Fruit {", "Fruit {\n" +
                "    \n" +
                "    private String color;\n" +
                "\n" +
                "    public String getColor() {\n" +
                "        return color;\n" +
                "    }\n" +
                "\n" +
                "    public Fruit setColor(String color) {\n" +
                "        this.color = color;\n" +
                "        return this;\n" +
                "    }"));
        //added a field, should now fail (if hibernate were still in charge this would work)
        RestAssured.get("fruit").then().statusCode(500);
        //now update out sql
        config.modifyResourceFile("db/create/V1.0.0__quarkus-flyway-deployment.sql", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s + "\nalter table FRUIT add column color VARCHAR;";
            }
        });
        RestAssured.get("fruit").then().statusCode(200)
                .body("[0].name", CoreMatchers.is("Orange"));
    }

    @Path("/fruit")
    @Startup
    public static class Endpoint {

        @Inject
        EntityManager entityManager;

        @Inject
        UserTransaction tx;

        @GET
        public List<Fruit> list() {
            return entityManager.createQuery("from Fruit").getResultList();
        }

        @PostConstruct
        @Transactional
        public void add() throws Exception {
            tx.begin();
            try {
                Fruit f = new Fruit();
                f.setName("Orange");
                entityManager.persist(f);
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }

    }
}
