package io.quarkus.smallrye.graphql.client.deployment.model;

import java.util.List;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class TestingGraphQLApi {

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

}
