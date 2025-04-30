package io.quarkus.qute;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Represents an immutable template definition.
 * <p>
 * The workflow is as follows:
 * <ol>
 * <li>Create a new template instance via {@link #instance()} or any convenient method</li>
 * <li>Set the model data</li>
 * <li>Trigger rendering with {@link TemplateInstance#render()} or any other convenient methods</li>
 * </ol>
 */
public interface Template {

    /**
     * Create a new template instance to configure the model data.
     *
     * @return a new template instance
     * @see TemplateInstance.Initializer
     */
    TemplateInstance instance();

    /**
     *
     * @param data
     * @return a new template instance
     * @see TemplateInstance#data(Object)
     */
    default TemplateInstance data(Object data) {
        return instance().data(data);
    }

    /**
     * @param key
     * @param data
     * @return a new template instance
     * @see TemplateInstance#data(String, Object)
     */
    default TemplateInstance data(String key, Object data) {
        return instance().data(key, data);
    }

    /**
     *
     * @param key1
     * @param data1
     * @param key2
     * @param data2
     * @return a new template instance
     */
    default TemplateInstance data(String key1, Object data1, String key2, Object data2) {
        return instance().data(key1, data1).data(key2, data2);
    }

    /**
     *
     * @param key1
     * @param data1
     * @param key2
     * @param data2
     * @param key3
     * @param data3
     * @return a new template instance
     */
    default TemplateInstance data(String key1, Object data1, String key2, Object data2, String key3, Object data3) {
        return instance().data(key1, data1).data(key2, data2).data(key3, data3);
    }

    /**
     *
     * @param key1
     * @param data1
     * @param key2
     * @param data2
     * @param key3
     * @param data3
     * @param key4
     * @param data4
     * @return a new template instance
     */
    default TemplateInstance data(String key1, Object data1, String key2, Object data2, String key3, Object data3, String key4,
            Object data4) {
        return instance().data(key1, data1).data(key2, data2).data(key3, data3).data(key4, data4);
    }

    /**
     *
     * @param key1
     * @param data1
     * @param key2
     * @param data2
     * @param key3
     * @param data3
     * @param key4
     * @param data4
     * @param key5
     * @param data5
     * @return a new template instance
     *
     */
    default TemplateInstance data(String key1, Object data1, String key2, Object data2, String key3, Object data3, String key4,
            Object data4, String key5, Object data5) {
        return instance().data(key1, data1).data(key2, data2).data(key3, data3).data(key4, data4).data(key5, data5);
    }

    default String render(Object data) {
        return data(data).render();
    }

    default String render() {
        return instance().render();
    }

    /**
     *
     * @return an immutable list of expressions used in the template
     */
    List<Expression> getExpressions();

    /**
     *
     * @param predicate
     * @return the first expression matching the given predicate or {@code null} if no such expression is used in the template
     */
    Expression findExpression(Predicate<Expression> predicate);

    /**
     * The id is unique for the engine instance.
     *
     * @return the generated id
     */
    String getGeneratedId();

    /**
     * An identifier used to obtain the template from the engine.
     *
     * @return the identifier
     * @see Engine#getTemplate(String)
     * @see Engine#parse(String, Variant, String)
     */
    String getId();

    /**
     * If invoked upon a fragment instance then delegate to the defining template.
     *
     * @return the template variant
     */
    Optional<Variant> getVariant();

    /**
     * Returns all type parameter declarations of the template, including the declarations added by a
     * {@link io.quarkus.qute.ParserHook}.
     * <p>
     * If invoked upon a fragment instance then delegate to the defining template.
     *
     * @return an immutable list of all type parameter declarations defined in the template
     */
    List<ParameterDeclaration> getParameterDeclarations();

    /**
     * Attempts to find the fragment with the specified identifier.
     * <p>
     * Note that fragment identifiers must be unique in a template.
     * <p>
     * If invoked upon a fragment instance then delegate to the defining template.
     *
     * @param id The fragment identifier
     * @return the fragment or {@code null}
     */
    Fragment getFragment(String id);

    /**
     * Returns an immutable set of identifiers of fragments defined in the template.
     * <p>
     * If invoked upon a fragment instance then delegate to the defining template.
     *
     * @return the set of fragment ids
     */
    Set<String> getFragmentIds();

    /**
     * @return {@code true} if this template is a fragment, {@code false} otherwise
     */
    default boolean isFragment() {
        return false;
    }

    /**
     * Returns the child nodes of the root node.
     *
     * @return the child nodes of the root node
     */
    List<TemplateNode> getNodes();

    /**
     * Returns all nodes of this template that match the given predicate.
     *
     * @param predicate
     * @return the collection of nodes that match the given predicate
     */
    Collection<TemplateNode> findNodes(Predicate<TemplateNode> predicate);

    /**
     *
     * @return the root section node
     */
    SectionNode getRootNode();

    /**
     * A fragment represents a part of the template that can be treated as a separate template.
     */
    interface Fragment extends Template {

        /**
         * An attibute with this key is added to a template instance of a fragment.
         *
         * @see TemplateInstance#setAttribute(String, Object)
         */
        String ATTRIBUTE = "qute$fragment";

        /**
         * Note that fragment identifiers must be unique in a template.
         *
         * @return the identifier of the fragment
         */
        @Override
        String getId();

        /**
         * @return the template this fragment originates from
         */
        Template getOriginalTemplate();

        @Override
        default boolean isFragment() {
            return true;
        }
    }

}
