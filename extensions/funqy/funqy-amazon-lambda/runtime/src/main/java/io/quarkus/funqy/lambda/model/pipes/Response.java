package io.quarkus.funqy.lambda.model.pipes;

import java.util.List;

public class Response {

    private final List<BatchItemFailures> batchItemFailures;

    public Response(final List<BatchItemFailures> batchItemFailures) {
        this.batchItemFailures = batchItemFailures;
    }

    public List<BatchItemFailures> getBatchItemFailures() {
        return batchItemFailures;
    }
}
