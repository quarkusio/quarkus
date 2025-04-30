package io.quarkus.qute;

import io.quarkus.qute.Expression.Part;
import io.quarkus.qute.TemplateNode.Origin;

/**
 * Represents a type parameter declaration, i.e. <code>{@org.acme.Foo foo}</code>.
 */
public interface ParameterDeclaration {

    /**
     * The type info for <code>{@org.acme.Foo foo}</code> is {@code |org.acme.Foo|}.
     *
     * @return the unparsed type info
     * @see Part#getTypeInfo()
     */
    String getTypeInfo();

    /**
     * The key for <code>{@org.acme.Foo foo}</code> is {@code foo}.
     *
     * @return the key
     */
    String getKey();

    /**
     *
     * @return the default value or {@code null} if no default value is set
     */
    Expression getDefaultValue();

    /**
     *
     * @return the origin of the template node
     */
    Origin getOrigin();

}
