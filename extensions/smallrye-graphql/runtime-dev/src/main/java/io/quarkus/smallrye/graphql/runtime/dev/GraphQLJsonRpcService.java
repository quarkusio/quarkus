package io.quarkus.smallrye.graphql.runtime.dev;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;

import graphql.schema.GraphQLSchema;
import io.quarkus.assistant.runtime.dev.Assistant;
import io.smallrye.graphql.execution.SchemaPrinter;

public class GraphQLJsonRpcService {
    private final SchemaPrinter schemaPrinter;

    @Inject
    GraphQLSchema graphQLSchema;

    @Inject
    Optional<Assistant> assistant;

    public GraphQLJsonRpcService() {
        this.schemaPrinter = new SchemaPrinter();
    }

    public String getGraphQLSchema() {
        return schemaPrinter.print(graphQLSchema);
    }

    public CompletionStage<Map<String, String>> generateClient(String language, String extraContext) {
        if (assistant.isPresent()) {
            String schemaDocument = getGraphQLSchema();

            return assistant.get().assistBuilder()
                    .userMessage(USER_MESSAGE)
                    .addVariable("schemaDocument", schemaDocument)
                    .addVariable("language", language)
                    .addVariable("extraContext", extraContext)
                    .assist();
        }
        return CompletableFuture.failedStage(new RuntimeException("Assistant is not available"));
    }

    private static final String USER_MESSAGE = """
            Given the GraphQL Schema document :
            {{schemaDocument}}
            Please generate a {{language}} Object that act as a client to all the operations in the schema.
            This {{language}} code must be able to be called like this (pseudo code):
            ```
            var stub = new ResourceNameHereClient();
            var response = stub.doOperation(someparam);
            ```
            Your reponse should only contain one field called `code` that contains a value with only the {{language}} code, nothing else, no explanation, and do not put the code in backticks.
            The {{language}} code must run and be valid.
            Example response: {code: 'package foo.bar; // more code here'}

            {{extraContext}}
            """;

}
