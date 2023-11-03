package io.quarkus.qute;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Represents an instance of {@link Template}.
 * <p>
 * This construct is not thread-safe.
 */
public interface TemplateInstance {

    /**
     * Attribute key - the timeout for {@link #render()} in milliseconds.
     *
     * @see #getTimeout()
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
    default TemplateInstance data(Object data) {
        throw new UnsupportedOperationException();
    }

    /**
     * Put the data in a map. The map will be used as the root context object during rendering. Invocation of this
     * method removes the root data object previously set by {@link #data(Object)}.
     *
     * @param key
     * @param data
     * @return self
     */
    default TemplateInstance data(String key, Object data) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param key
     * @param value
     * @return self
     */
    default TemplateInstance setAttribute(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param key
     * @return the attribute or null
     */
    default Object getAttribute(String key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Triggers rendering. Note that this method blocks the current thread!
     *
     * @return the rendered template as string
     */
    default String render() {
        throw new UnsupportedOperationException();
    }

    /**
     * Triggers rendering.
     *
     * @return a completion stage that is completed once the rendering finished
     */
    default CompletionStage<String> renderAsync() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new {@link Multi} that can be used to consume chunks of the rendered template. In particular, each item
     * represents a part of the rendered template.
     * <p>
     * This operation does not trigger rendering. Instead, each subscription triggers a new rendering of the template.
     *
     * @return a new Multi
     * @see Multi#subscribe()
     */
    default Multi<String> createMulti() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new {@link Uni} that can be used to consume the rendered template.
     * <p>
     * This operation does not trigger rendering. Instead, each subscription triggers a new rendering of the template.
     *
     * @return a new Uni
     * @see Uni#subscribe()
     */
    default Uni<String> createUni() {
        throw new UnsupportedOperationException();
    }

    /**
     * Triggers rendering.
     *
     * @param consumer To consume chunks of the rendered template
     * @return a completion stage that is completed once the rendering finished
     */
    default CompletionStage<Void> consume(Consumer<String> consumer) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @return the timeout
     * @see TemplateInstance#TIMEOUT
     */
    default long getTimeout() {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @return the original template
     */
    default Template getTemplate() {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param id
     * @return the fragment or {@code null}
     * @see Template#getFragment(String)
     */
    default Template getFragment(String id) {
        return getTemplate().getFragment(id);
    }

    /**
     * Register an action that is performed after the rendering is finished.
     *
     * @param action
     * @return self
     */
    default TemplateInstance onRendered(Runnable action) {
        throw new UnsupportedOperationException();
    }

    /**
     * This component can be used to initialize a template instance, i.e. the data and attributes.
     *
     * @see TemplateInstance#data(String, Object)
     * @see TemplateInstance#setAttribute(String, Object)
     */
    interface Initializer extends Consumer<TemplateInstance> {

    }

}
