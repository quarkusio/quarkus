package io.quarkus.hibernate.accessor.test.devmode;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Accessor hosts are numbered by their position in the sorted host list, and that number is baked into
 * the bytecode injected in the host. Adding a host that sorts before an existing one renumbers every
 * later host, so a host transformation that is reused from the live reload cache would dispatch to the
 * wrong class.
 */
public class HibernateAccessorDevModeReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root.addClasses(
                    ReloadAccessorEndpoint.class,
                    MmmReloadEntity.class,
                    ZzzReloadEntity.class));

    @Test
    void accessorsRemainCorrectWhenAHostSortingFirstIsAdded() {
        RestAssured.get("/accessor/read").then().statusCode(200).body(is("mmm"));
        RestAssured.get("/accessor/write").then().statusCode(200).body(is("written"));

        TEST.addSourceFile(AaaReloadEntity.class);

        RestAssured.get("/accessor/read").then().statusCode(200).body(is("mmm"));
        RestAssured.get("/accessor/write").then().statusCode(200).body(is("written"));
    }
}
