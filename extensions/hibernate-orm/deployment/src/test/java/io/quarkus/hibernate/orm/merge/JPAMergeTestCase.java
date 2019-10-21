/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package io.quarkus.hibernate.orm.merge;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JPAMergeTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestEntity.class, JPATestMergeResource.class).addAsResource(new StringAsset(""), "import.sql")); // define an empty import.sql file

    @Test
    public void testMergeFromTrueToFalse() {
        TestEntity testEntity = new TestEntity();
        testEntity.setaBoolean(true);

        testEntity = given()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .body(testEntity)
                .when()
                .get("/testresource/persist")
                .then()
                .extract().as(TestEntity.class);

        testEntity.setaBoolean(false);

        given().body(testEntity)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("/testresource/merge");

        testEntity = given()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .get("/testresource/find?id=" + testEntity.getId())
                .then()
                .extract().as(TestEntity.class);

        assertFalse(testEntity.isaBoolean());
    }
}
