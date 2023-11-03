package io.quarkus.it.resteasy.reactive.elytron;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class FruitResourceTest {

    public static final TypeRef<List<Fruit>> FRUIT_LIST_TYPE_REF = new TypeRef<>() {
    };
    public static final TypeRef<Fruit> FRUIT_TYPE_REF = new TypeRef<>() {
    };

    @Test
    void testAllNoSecurity() {
        String path = "all-no-security";
        List<Fruit> noUserResponse = verifySuccessAndExtractFruits(path, null, FRUIT_LIST_TYPE_REF);
        assertThat(noUserResponse).containsOnly(Fruit.APPLE, Fruit.PINEAPPLE);

        List<Fruit> employeeResponse = verifySuccessAndExtractFruits(path, "john", FRUIT_LIST_TYPE_REF);
        assertThat(employeeResponse).containsOnly(Fruit.APPLE, Fruit.PINEAPPLE);

        List<Fruit> managerResponse = verifySuccessAndExtractFruits(path, "mary", FRUIT_LIST_TYPE_REF);
        assertThat(managerResponse).containsOnly(Fruit.APPLE, Fruit.PINEAPPLE);
    }

    @Test
    void testAllWithSecurity() {
        String path = "all-with-security";
        List<Fruit> noUserResponse = verifySuccessAndExtractFruits(path, null, FRUIT_LIST_TYPE_REF);
        assertThat(noUserResponse).hasSize(2).allSatisfy(f -> {
            assertThat(f.getDescription()).isNull();
            assertThat(f.getId()).isNull();
        }).extracting("name").containsOnly(Fruit.APPLE.getName(), Fruit.PINEAPPLE.getName());

        List<Fruit> employeeResponse = verifySuccessAndExtractFruits(path, "john", FRUIT_LIST_TYPE_REF);
        assertThat(employeeResponse).hasSize(2).allSatisfy(f -> {
            assertThat(f.getDescription()).isNull();
            assertThat(f.getId()).isPositive();
        }).extracting("name").containsOnly(Fruit.APPLE.getName(), Fruit.PINEAPPLE.getName());

        List<Fruit> managerResponse = verifySuccessAndExtractFruits(path, "mary", FRUIT_LIST_TYPE_REF);
        assertThat(managerResponse).containsOnly(Fruit.APPLE, Fruit.PINEAPPLE);

        List<Fruit> internResponse = verifySuccessAndExtractFruits(path, "poul", FRUIT_LIST_TYPE_REF);
        assertThat(internResponse).hasSize(2).allSatisfy(f -> {
            assertThat(f.getDescription()).isNull();
            assertThat(f.getId()).isNull();
        }).extracting("name").containsOnly(Fruit.APPLE.getName(), Fruit.PINEAPPLE.getName());
    }

    @Test
    void testSingleNoSecurity() {
        String path = "single-no-security";
        Fruit noUserResponse = verifySuccessAndExtractFruits(path, null, FRUIT_TYPE_REF);
        assertThat(noUserResponse).isEqualTo(Fruit.APPLE);

        Fruit employeeResponse = verifySuccessAndExtractFruits(path, "john", FRUIT_TYPE_REF);
        assertThat(employeeResponse).isEqualTo(Fruit.APPLE);

        Fruit managerResponse = verifySuccessAndExtractFruits(path, "mary", FRUIT_TYPE_REF);
        assertThat(managerResponse).isEqualTo(Fruit.APPLE);
    }

    @Test
    void testSingleWithSecurity() {
        String path = "single-with-security";
        Fruit noUserResponse = verifySuccessAndExtractFruits(path, null, FRUIT_TYPE_REF);
        assertThat(noUserResponse.getDescription()).isNull();
        assertThat(noUserResponse.getId()).isNull();
        assertThat(noUserResponse.getName()).isEqualTo(Fruit.APPLE.getName());

        Fruit employeeResponse = verifySuccessAndExtractFruits(path, "john", FRUIT_TYPE_REF);
        assertThat(employeeResponse.getDescription()).isNull();
        assertThat(employeeResponse.getId()).isEqualTo(Fruit.APPLE.getId());
        assertThat(employeeResponse.getName()).isEqualTo(Fruit.APPLE.getName());

        Fruit managerResponse = verifySuccessAndExtractFruits(path, "mary", FRUIT_TYPE_REF);
        assertThat(managerResponse).isEqualTo(Fruit.APPLE);

        Fruit internResponse = verifySuccessAndExtractFruits(path, "poul", FRUIT_TYPE_REF);
        assertThat(internResponse.getDescription()).isNull();
        assertThat(internResponse.getId()).isNull();
        assertThat(internResponse.getName()).isEqualTo(Fruit.APPLE.getName());
    }

    private <T> T verifySuccessAndExtractFruits(String path, String user, TypeRef<T> typeRef) {
        RequestSpecification given = given();
        if (user != null) {
            given = given.auth().preemptive().basic(user, Users.password(user));
        }
        return given
                .when()
                .get("/fruit/" + path)
                .then()
                .statusCode(200)
                .extract().body().as(typeRef);
    }
}
