package io.quarkus.qute;

import java.util.List;
import java.util.Optional;

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
     *
     * @return the template variant
     */
    Optional<Variant> getVariant();

    /**
     *
     * @return an immutable list of parameter declarations
     */
    List<ParameterDeclaration> getParameterDeclarations();

}
