package io.quarkus.qute;

import io.quarkus.qute.TemplateNode.Origin;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Represents a value expression. It could be a literal such as {@code 'foo'}. It could have a namespace such as {@code data}
 * for {@code data:name}. It could have several parts such as {@code item} and {@code name} for {@code item.name}.
 * 
 * @see Evaluator
 */
public interface Expression {

    /**
     * 
     * @return the namespace, may be {@code null}
     * @see NamespaceResolver
     */
    String getNamespace();

    default boolean hasNamespace() {
        return getNamespace() != null;
    }

    /**
     * 
     * @return the list of parts, is never {@code null}
     */
    List<Part> getParts();

    /**
     * 
     * @return true if it represents a literal
     */
    boolean isLiteral();

    /**
     * 
     * @return the literal value, or null
     */
    CompletableFuture<Object> getLiteralValue();

    /**
     * 
     * @return the origin
     */
    Origin getOrigin();

    /**
     * 
     * @return the original value as defined in the template
     */
    String toOriginalString();

    default String collectTypeInfo() {
        if (!hasTypeInfo()) {
            return null;
        }
        return getParts().stream().map(Part::getTypeInfo).collect(Collectors.joining("."));
    }

    default boolean hasTypeInfo() {
        return getParts().get(0).getTypeInfo() != null;
    }

    /**
     * The id must be unique for the template.
     * 
     * @return the generated id or {@code -1} for an expression that was not created by a parser
     */
    int getGeneratedId();

    /**
     * 
     * @see Expression#getParts()
     */
    interface Part {

        /**
         * 
         * @return the name of a property or virtual method
         */
        String getName();

        /**
         * An expression part may have a "type check information" attached. The string can be one of the following:
         * <ul>
         * <li>type info that represents a fully qualified type name (including type parameters) -
         * {@code |TYPE_INFO|<section-hint>};
         * for example {@code |org.acme.Foo|},
         * {@code |java.util.List<org.acme.Label>|} and {@code |org.acme.Foo|<when#123>}</li>
         * <li>property; for example {@code foo} and {@code foo<loop#123>}</li>
         * <li>virtual method; for example {@code foo.call(bar)} and {@code foo.getNames(10)<loop-element>}</li>
         * </ul>
         * 
         * @return the type check info
         */
        String getTypeInfo();

        default boolean isVirtualMethod() {
            return false;
        }

        default VirtualMethodPart asVirtualMethod() {
            throw new IllegalStateException("Not a virtual method: " + toString() + " [typeInfo: " + getTypeInfo() + "]");
        }

    }

    /**
     * Part that represents a virtual method.
     */
    interface VirtualMethodPart extends Part {

        List<Expression> getParameters();

    }

}
