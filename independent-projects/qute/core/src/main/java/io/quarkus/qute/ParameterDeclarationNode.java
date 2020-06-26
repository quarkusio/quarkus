package io.quarkus.qute;

import java.util.function.Consumer;

/**
 * Template parameter declaration.
 */
public class ParameterDeclarationNode extends TextNode {

    public ParameterDeclarationNode(String value, Origin origin) {
        super(value, origin);
    }

    @Override
    public void process(Consumer<String> consumer) {
    }

}
