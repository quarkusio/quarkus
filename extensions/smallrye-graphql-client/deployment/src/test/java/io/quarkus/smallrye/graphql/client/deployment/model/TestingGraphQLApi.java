package io.quarkus.smallrye.graphql.client.deployment.model;

import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.graphql.execution.context.SmallRyeContext;

@GraphQLApi
public class TestingGraphQLApi {

    @Inject
    CurrentVertxRequest request;

    @Inject
    SmallRyeContext ctx;

    @Query
    public List<Person> people() {
        Person person1 = new Person();
        person1.setFirstName("John");
        person1.setLastName("Marston");

        Person person2 = new Person();
        person2.setFirstName("Arthur");
        person2.setLastName("Morgan");

        return List.of(person1, person2);
    }

    /**
     * Returns the value of the HTTP header denoted by 'key'.
     */
    @Query
    public String returnHeader(String key) {
        return request.getCurrent().request().getHeader(key);
    }

    private final String EXPECTED_QUERY_FOR_NAME_OF_THE_PERSON = "query nameOfThePerson($personDto: PersonInput) { nameOfThePerson(personDto: $personDto) }";

    @Query
    public String getNameOfThePerson(PersonDto personDto) {
        if (!ctx.getQuery().equals(EXPECTED_QUERY_FOR_NAME_OF_THE_PERSON))
            throw new RuntimeException(
                    String.format("Wrong Query - expected: %s\n actual: %s", EXPECTED_QUERY_FOR_NAME_OF_THE_PERSON,
                            ctx.getQuery()));
        return personDto.getName();
    }
}
