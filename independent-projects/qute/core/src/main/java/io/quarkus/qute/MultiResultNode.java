package io.quarkus.qute;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * A result node backed by an array of result nodes.
 */
public class MultiResultNode implements ResultNode {

    private final ResultNode[] results;

    public MultiResultNode(CompletableFuture<ResultNode>[] futures) {
        ResultNode[] results = new ResultNode[futures.length];
        for (int i = 0; i < futures.length; i++) {
            try {
                results[i] = futures[i].get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }
        this.results = results;
    }

    @Override
    public void process(Consumer<String> consumer) {
        for (ResultNode result : results) {
            result.process(consumer);
        }
    }

}
