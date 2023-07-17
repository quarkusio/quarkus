package io.quarkus.qute;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Template parameter declaration.
 */
public class ParameterDeclarationNode implements TemplateNode, ParameterDeclaration {

    private final String typeInfo;
    private final String key;
    private final Expression defaultValue;
    private final Origin origin;

    public ParameterDeclarationNode(String typeInfo, String key, Expression defaultValue, Origin origin) {
        this.typeInfo = typeInfo;
        this.key = key;
        this.defaultValue = defaultValue;
        this.origin = origin;
    }

    @Override
    public CompletionStage<ResultNode> resolve(ResolutionContext context) {
        return ResultNode.NOOP;
    }

    public String getTypeInfo() {
        return typeInfo;
    }

    public String getKey() {
        return key;
    }

    public Expression getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Origin getOrigin() {
        return origin;
    }

    @Override
    public List<ParameterDeclaration> getParameterDeclarations() {
        return Collections.singletonList(this);
    }

}
