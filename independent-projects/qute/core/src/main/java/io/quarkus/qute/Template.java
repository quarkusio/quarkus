package io.quarkus.qute;

import java.util.Optional;
import java.util.Set;

/**
 * Represents an immutable template definition.
 * <p>
 * The workflow is as follows:
 * <ol>
 * <li>Create a new template instance via {@link #instance()} or any convenient method</li>
 * <li>Set the model data</li>
 * <li>Trigger rendering with {@link TemplateInstance#render()}, {@link TemplateInstance#renderAsync()},
 * {@link TemplateInstance#consume(java.util.function.Consumer)} or subscribe to a publisher returned from
 * {@link TemplateInstance#publisher()}</li>
 * </ol>
 */
public interface Template {

    /**
     * Template instance represents a rendering configuration.
     * 
     * @return a new template instance
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

    default String render(Object data) {
        return data(data).render();
    }

    default String render() {
        return instance().render();
    }

    /**
     * 
     * @return an immutable set of expressions used in the template
     */
    Set<Expression> getExpressions();

    /**
     * The id is unique for the engine instance.
     * 
     * @return the generated id
     */
    String getGeneratedId();

    /**
     * 
     * @return the template variant
     */
    Optional<Variant> getVariant();

}
