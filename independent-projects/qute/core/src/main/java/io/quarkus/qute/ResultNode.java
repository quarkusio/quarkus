package io.quarkus.qute;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Node of a result tree.
 */
public interface ResultNode {

    static CompletableFuture<ResultNode> NOOP = CompletableFuture.completedFuture(new ResultNode() {
        @Override
        public void process(Consumer<String> resultConsumer) {
        }
    });

    /**
     * 
     * @param resultConsumer
     */
    void process(Consumer<String> resultConsumer);

}
