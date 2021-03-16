package io.quarkus.qute;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Represents an instance of {@link Template}.
 * <p>
 * This construct is not thread-safe.
 */
public interface TemplateInstance {

    /**
     * Attribute key - the timeout for {@link #render()} in milliseconds.
     */
    String TIMEOUT = "timeout";

    /**
     * Attribute key - all template variants found.
     */
    String VARIANTS = "variants";

    /**
     * Attribute key - a selected variant.
     */
    String SELECTED_VARIANT = "selectedVariant";

    /**
     * Set the the root data object. Invocation of this method removes any data set previously by
     * {@link #data(String, Object)}.
     * 
     * @param data
     * @return
     */
    TemplateInstance data(Object data);

    /**
     * Put the data in a map. The map will be used as the root context object during rendering. Invocation of this
     * method removes the root data object previously set by {@link #data(Object)}.
     * 
     * @param key
     * @param data
     * @return self
     */
    TemplateInstance data(String key, Object data);

    /**
     * 
     * @param key
     * @param value
     * @return self
     */
    TemplateInstance setAttribute(String key, Object value);

    /**
     * 
     * @param key
     * @return the attribute or null
     */
    Object getAttribute(String key);

    /**
     * Triggers rendering. Note that this method blocks the current thread!
     * 
     * @return the rendered template as string
     */
    String render();

    /**
     * Triggers rendering.
     * 
     * @return a completion stage that is completed once the rendering finished
     */
    CompletionStage<String> renderAsync();

    /**
     * Create a new {@link Multi} that can be used to consume chunks of the rendered template. In particular, each item
     * represents a part of the rendered template.
     * <p>
     * This operation does not trigger rendering. Instead, each subscription triggers a new rendering of the template.
     * 
     * @return a new Multi
     * @see Multi#subscribe()
     */
    Multi<String> createMulti();

    /**
     * Create a new {@link Uni} that can be used to consume the rendered template.
     * <p>
     * This operation does not trigger rendering. Instead, each subscription triggers a new rendering of the template.
     * 
     * @return a new Uni
     * @see Uni#subscribe()
     */
    Uni<String> createUni();

    /**
     * Triggers rendering.
     * 
     * @param consumer To consume chunks of the rendered template
     * @return a completion stage that is completed once the rendering finished
     */
    CompletionStage<Void> consume(Consumer<String> consumer);

}
