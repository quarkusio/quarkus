package io.quarkus.smallrye.graphql.deployment.federation.resolver;

import org.eclipse.microprofile.graphql.GraphQLApi;

import io.smallrye.graphql.api.federation.Resolver;

@GraphQLApi
public class ExtendedApi {
    @Resolver
    public ExtendedType extendedTypeById(String id) {
        ExtendedType extendedType = new ExtendedType();
        extendedType.setId(id);
        extendedType.setDescription("extendedTypeById");
        return extendedType;
    }

    @Resolver
    public ExtendedType extendedTypeByIdNameKey(String id, String name, String key) {
        ExtendedType extendedType = new ExtendedType();
        extendedType.setId(id);
        extendedType.setValue(id + name + key);
        extendedType.setDescription("extendedTypeByIdNameKey");
        return extendedType;
    }
}
