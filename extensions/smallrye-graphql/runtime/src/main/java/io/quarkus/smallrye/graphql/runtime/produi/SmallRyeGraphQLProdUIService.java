package io.quarkus.smallrye.graphql.runtime.produi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.graphql.execution.SchemaPrinter;

/**
 * Read-only Prod UI view of the running application's GraphQL schema. It exposes
 * the generated schema document (SDL) and the list of operations (queries,
 * mutations and subscriptions) with their arguments and return types, derived
 * from the always-present {@link GraphQLSchema} bean. It deliberately omits the
 * Dev UI's GraphiQL execution client: there is no way to run a query, mutation
 * or subscription from this view.
 */
@ApplicationScoped
public class SmallRyeGraphQLProdUIService {

    @Inject
    GraphQLSchema graphQLSchema;

    private final SchemaPrinter schemaPrinter = new SchemaPrinter();

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get the running application's GraphQL schema document (SDL)")
    public String getSchema() {
        return schemaPrinter.print(graphQLSchema);
    }

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only list of the GraphQL operations (queries, mutations and subscriptions)")
    public List<OperationInfo> getOperations() {
        List<OperationInfo> operations = new ArrayList<>();
        collect(operations, "Query", graphQLSchema.getQueryType());
        collect(operations, "Mutation", graphQLSchema.getMutationType());
        collect(operations, "Subscription", graphQLSchema.getSubscriptionType());
        return operations;
    }

    private void collect(List<OperationInfo> operations, String kind, GraphQLObjectType type) {
        if (type == null) {
            return;
        }
        for (GraphQLFieldDefinition field : type.getFieldDefinitions()) {
            String arguments = field.getArguments().stream()
                    .map(this::describeArgument)
                    .collect(Collectors.joining(", "));
            operations.add(new OperationInfo(kind, field.getName(), arguments,
                    GraphQLTypeUtil.simplePrint(field.getType()), field.getDescription()));
        }
    }

    private String describeArgument(GraphQLArgument argument) {
        return argument.getName() + ": " + GraphQLTypeUtil.simplePrint(argument.getType());
    }

    public record OperationInfo(String kind, String name, String arguments, String returnType, String description) {
    }
}
