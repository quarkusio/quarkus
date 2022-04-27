package io.quarkus.smallrye.graphql.client.deployment.model;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import io.quarkus.smallrye.graphql.runtime.spi.datafetcher.ContextHelper;

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

    /**
     * Returns the value of the HTTP header denoted by 'key'.
     */
    @Query
    public String returnHeader(String key) {
        Map<String, List<String>> httpHeaders = ContextHelper.getHeaders();
        List<String> allvalues = httpHeaders.get(key);
        if (allvalues.size() == 1) {
            return allvalues.get(0);
        }
        throw new RuntimeException("Unexpected number of value for header [" + key + "]: " + allvalues.size());
    }

}
