package io.quarkus.qute;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A result node backed by an array of result nodes.
 */
public final class MultiResultNode implements ResultNode {

    private final Supplier<ResultNode>[] results;

    public MultiResultNode(Supplier<ResultNode>[] results) {
        this.results = results;
    }

    @Override
    public void process(Consumer<String> consumer) {
        for (Supplier<ResultNode> result : results) {
            result.get().process(consumer);
        }
    }

}
