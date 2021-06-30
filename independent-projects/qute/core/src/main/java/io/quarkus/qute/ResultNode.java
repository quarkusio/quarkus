package io.quarkus.qute;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Node of a result tree.
 */
public interface ResultNode {

    static CompletionStage<ResultNode> NOOP = CompletedStage.of(new ResultNode() {
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
