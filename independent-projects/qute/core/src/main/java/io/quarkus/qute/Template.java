package io.quarkus.qute;

import java.util.Optional;
import java.util.Set;

/**
 * Represents a template definition.
 */
public interface Template {

    /**
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
