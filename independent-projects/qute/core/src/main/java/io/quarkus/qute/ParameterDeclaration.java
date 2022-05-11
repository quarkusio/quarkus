package io.quarkus.qute;

/**
 * Represents a parameter declaration, i.e. <code>{@org.acme.Foo foo}</code>.
 */
public interface ParameterDeclaration {

    String getTypeInfo();

    String getKey();

    Expression getDefaultValue();

}
