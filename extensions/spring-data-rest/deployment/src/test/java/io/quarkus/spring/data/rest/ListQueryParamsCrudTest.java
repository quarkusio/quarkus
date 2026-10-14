package io.quarkus.spring.data.rest;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.QueryParam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.HashUtil;
import io.quarkus.spring.data.rest.crud.DefaultRecordsRepository;
import io.quarkus.spring.data.rest.crud.Record;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Spring Data REST resources support sorting by entity fields, but not filtering by them, so the generated list method
 * must not declare a query parameter per entity field.
 */
class ListQueryParamsCrudTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractEntity.class, Record.class, DefaultRecordsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    void shouldNotDeclareEntityFieldsAsFilterQueryParams() throws ClassNotFoundException {
        String repository = DefaultRecordsRepository.class.getName();
        Class<?> resource = Thread.currentThread().getContextClassLoader()
                .loadClass(repository + "JaxRs_" + HashUtil.sha1(repository));

        Set<String> queryParams = Arrays.stream(resource.getDeclaredMethods())
                .filter(m -> m.getName().equals("list"))
                .map(Method::getParameters)
                .flatMap(Arrays::stream)
                .map(p -> p.getAnnotation(QueryParam.class))
                .filter(a -> a != null)
                .map(QueryParam::value)
                .collect(Collectors.toSet());

        assertThat(queryParams).containsExactlyInAnyOrder("sort", "namedQuery");
    }

    @Test
    void shouldSortByEntityField() {
        given().accept("application/json")
                .when().get("/default-records?sort=name")
                .then().statusCode(200);
    }

    @Test
    void shouldNotSortByUnknownField() {
        given().accept("application/json")
                .when().get("/default-records?sort=unknownField")
                .then().statusCode(400);
    }
}
