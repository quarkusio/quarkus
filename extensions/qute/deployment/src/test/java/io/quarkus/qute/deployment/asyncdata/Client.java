package io.quarkus.qute.deployment.asyncdata;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Client {

    public CompletionStage<List<String>> getTokens() {
        CompletableFuture<List<String>> tokens = new CompletableFuture<>();
        tokens.complete(Arrays.asList("alpha", "bravo", "delta"));
        return tokens;
    }

}
