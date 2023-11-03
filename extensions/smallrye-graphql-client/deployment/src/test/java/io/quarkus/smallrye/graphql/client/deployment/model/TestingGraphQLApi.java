package io.quarkus.smallrye.graphql.client.deployment.model;

import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

@GraphQLApi
public class TestingGraphQLApi {

    @Inject
    CurrentVertxRequest request;

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

}
