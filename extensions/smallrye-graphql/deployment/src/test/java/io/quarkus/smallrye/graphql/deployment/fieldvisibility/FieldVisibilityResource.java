package io.quarkus.smallrye.graphql.deployment.fieldvisibility;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

// It is important to note that some of the fields are invisible in the GraphQL schema,
// this is because they have been explicitly hidden via the
// quarkus.smallrye-graphql.field-visibility property.

@GraphQLApi
public class FieldVisibilityResource {

    public static class Book {
        public String title; // hidden in the schema
        public String author;
    }

    public static class Customer {
        public String name; // hidden in the schema
        public String address;
    }

    public static class Purchase {
        public Book book;
        public Customer customer;
        public int count; // hidden in the schema
    }

    @Query
    public Purchase someFirstQuery(Book book, Customer customer, Purchase purchase) {
        return purchase;
    }

    @Query
    public Customer someSecondQuery() {
        return null;
    }

    @Query
    public Book someThirdQuery() {
        return null;
    }
}
