package io.quarkus.smallrye.openapi.runtime.dev;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;

import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.quarkus.smallrye.openapi.runtime.OpenApiDocumentService;
import io.smallrye.openapi.runtime.io.Format;

public class OpenApiJsonRpcService {

    @Inject
    Optional<Assistant> assistant;

    @Inject
    OpenApiDocumentService openApiDocumentService;

    @JsonRpcDescription("Get the running application's OpenAPI Schema Document in json format")
    @DevMCPEnableByDefault
    public String getOpenAPISchema() {
        // For now client generation is only possible for the default document
        return new String(openApiDocumentService.getDocument(OpenApiConstants.DEFAULT_DOCUMENT_NAME, Format.JSON));
    }

    public CompletionStage<Map<String, String>> generateClient(String language, String extraContext) {
        if (assistant.isPresent()) {
            String schemaDocument = getOpenAPISchema();

            return assistant.get().assistBuilder()
                    .userMessage(USER_MESSAGE)
                    .addVariable("schemaDocument", schemaDocument)
                    .addVariable("language", language)
                    .addVariable("extraContext", extraContext)
                    .responseType(OpenApiClientResponse.class)
                    .assist();
        }
        return CompletableFuture.failedStage(new RuntimeException("Assistant is not available"));
    }

    private static final String USER_MESSAGE = """
            Given the OpenAPI Schema document :

            ```json
            {{schemaDocument}}
            ```

            Please generate a {{language}} Object that act as a client to all the operations in the schema.
            This {{language}} code must be able to be called like this (pseudo code):

            ```
            var stub = new ResourceNameHereClient();
            var response = stub.doOperation(someparam);
            ```

            Don't use ResourceNameHereClient as the name for the generated code (it's just an example). Derive a sensible name from the schema provided.
            Your response should only contain one field called `code` that contains a value with only the {{language}} code, nothing else, no explanation, and do not put the code in backticks.
            The {{language}} code must run and be valid.

            Example response: `{code: 'package foo.bar; // more code here'}`

            {{extraContext}}
            """;

    final record OpenApiClientResponse(String code) {
    }
}
