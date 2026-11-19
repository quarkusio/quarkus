package io.quarkus.qute;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Node of a result tree.
 */
public abstract class ResultNode {

    public static CompletionStage<ResultNode> NOOP = CompletedStage.of(new ResultNode() {
        @Override
        public void process(Consumer<String> resultConsumer) {
        }

        @Override
        public void process(ChunkWriter writer) {
        }
    });

    /**
     *
     * @param resultConsumer
     */
    public abstract void process(Consumer<String> resultConsumer);

    /**
     * Processes the result tree using the chunked output. Static text may be handed over to the writer pre-encoded, see
     * {@link ChunkWriter#write(byte[])}.
     * <p>
     * The default implementation falls back to the {@link String}-based processing, i.e. a custom result node that only
     * implements {@link #process(Consumer)} never emits pre-encoded chunks but keeps working.
     *
     * @param writer
     */
    public void process(ChunkWriter writer) {
        process(writer::write);
    }

}
