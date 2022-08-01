package io.quarkus.qute;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Static text.
 */
public class TextNode implements TemplateNode, ResultNode {

    private final CompletedStage<ResultNode> result;
    private final String value;
    private final Origin origin;

    public TextNode(String value, Origin origin) {
        this.result = CompletedStage.of(this);
        this.value = value;
        this.origin = origin;
    }

    @Override
    public CompletionStage<ResultNode> resolve(ResolutionContext context) {
        return result;
    }

    public Origin getOrigin() {
        return origin;
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TextNode [value=").append(value).append("]");
        return builder.toString();
    }

}