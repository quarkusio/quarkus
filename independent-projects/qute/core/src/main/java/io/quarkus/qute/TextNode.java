package io.quarkus.qute;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Static text.
 */
public class TextNode extends ResultNode implements TemplateNode {

    private final CompletedStage<ResultNode> result;
    private final String value;
    private final Origin origin;
    // Lazily encoded UTF-8 value; the volatile write publishes the array contents to concurrent readers
    private volatile byte[] utf8Value;

    public TextNode(String value, Origin origin) {
        this.result = CompletedStage.of(this);
        this.value = value;
        this.origin = origin;
    }

    @Override
    public CompletionStage<ResultNode> resolve(ResolutionContext context) {
        return result;
    }

    @Override
    public Origin getOrigin() {
        return origin;
    }

    @Override
    public Kind kind() {
        return Kind.TEXT;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void process(Consumer<String> consumer) {
        consumer.accept(value);
    }

    @Override
    public void process(ChunkWriter writer) {
        if (writer.charset() == StandardCharsets.UTF_8) {
            // Static text never changes - encode it once and reuse the byte array afterwards
            byte[] encoded = utf8Value;
            if (encoded == null) {
                encoded = value.getBytes(StandardCharsets.UTF_8);
                utf8Value = encoded;
            }
            writer.write(encoded);
        } else {
            writer.write(value);
        }
    }

    @Override
    public TextNode asText() {
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TextNode [value=").append(value).append("]");
        return builder.toString();
    }

}