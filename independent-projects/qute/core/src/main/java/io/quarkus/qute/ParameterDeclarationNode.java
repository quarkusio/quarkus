package io.quarkus.qute;

import java.util.concurrent.CompletionStage;

/**
 * Template parameter declaration.
 * <p>
 * This node is only used when removing standalone lines.
 */
public class ParameterDeclarationNode implements TemplateNode {

    private final String value;
    private final Origin origin;

    public ParameterDeclarationNode(String value, Origin origin) {
        this.value = value;
        this.origin = origin;
    }

    @Override
    public CompletionStage<ResultNode> resolve(ResolutionContext context) {
        throw new UnsupportedOperationException();
    }

    public String getValue() {
        return value;
    }

    @Override
    public Origin getOrigin() {
        return origin;
    }

}
