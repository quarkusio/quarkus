package io.quarkus.smallrye.graphql.runtime;

import java.util.Currency;

import graphql.scalars.ExtendedScalars;
import graphql.scalars.country.code.CountryCode;
import graphql.schema.GraphQLScalarType;

public enum ExtraScalar {

    UUID(java.util.UUID.class, ExtendedScalars.UUID),
    URL(java.net.URL.class, ExtendedScalars.Url),
    LOCALE(java.util.Locale.class, ExtendedScalars.Locale),
    COUNTRY_CODE(CountryCode.class, ExtendedScalars.CountryCode),
    CURRENCY(Currency.class, ExtendedScalars.Currency);

    private final Class<?> valueClass;
    private final GraphQLScalarType graphQLScalarType;

    ExtraScalar(Class<?> valueClass, GraphQLScalarType graphQLScalarType) {
        this.valueClass = valueClass;
        this.graphQLScalarType = graphQLScalarType;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    public GraphQLScalarType getGraphQLScalarType() {
        return graphQLScalarType;
    }
}
