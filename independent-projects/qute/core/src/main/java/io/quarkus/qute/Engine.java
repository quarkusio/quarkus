package io.quarkus.qute;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import io.quarkus.qute.TemplateLocator.TemplateLocation;

/**
 * Represents a central point for template management.
 * <p>
 * It has a dedicated configuration and is able to cache the template definitions.
 *
 * @see EngineBuilder
 */
public interface Engine extends ErrorInitializer {

    /**
     *
     * @return a new builder instance
     */
    static EngineBuilder builder() {
        return new EngineBuilder();
    }

    /**
     * Parse the template contents.
     * <p>
     * Note that this method always returns a new {@link Template} instance.
     *
     * @param content
     * @return the template
     */
    default Template parse(String content) {
        return parse(content, null, null);
    }

    /**
     * Parse the template contents with the specified variant.
     * <p>
     * Note that this method always returns a new {@link Template} instance.
     *
     * @param content
     * @param variant
     * @return the template
     */
    default Template parse(String content, Variant variant) {
        return parse(content, variant, null);
    }

    /**
     * Parse the template contents with the specified variant and id.
     * <p>
     * Note that this method always returns a new {@link Template} instance.
     *
     * @param content
     * @param variant
     * @param id
     * @return the template
     */
    Template parse(String content, Variant variant, String id);

    /**
     *
     * @return an immutable list of result mappers
     */
    List<ResultMapper> getResultMappers();

    /**
     * Maps the given result to a string value. If no result mappers are available the {@link Object#toString()} value is used.
     *
     * @param result Must not be null
     * @param expression Must not be null
     * @return the string value
     * @see Engine#getResultMappers()
     */
    String mapResult(Object result, Expression expression);

    /**
     * A valid identifier is a sequence of non-whitespace characters.
     *
     * @param id
     * @param template
     * @return the previous value or null
     */
    Template putTemplate(String id, Template template);

    /**
     * Obtain a template for the given identifier. A template may be registered using
     * {@link #putTemplate(String, Template)} or loaded by a template locator.
     *
     * @param id
     * @return the template or null
     * @see EngineBuilder#addLocator(TemplateLocator)
     */
    Template getTemplate(String id);

    /**
     * Note that template locators are not used in this method.
     *
     * @param id
     * @return {@code true} if a template with the given identifier is loaded, {@code false} otherwise
     */
    boolean isTemplateLoaded(String id);

    /**
     * Removes all templates from the cache.
     */
    void clearTemplates();

    /**
     * Removes the templates for which the mapping id matches the given predicate.
     *
     * @param test
     */
    void removeTemplates(Predicate<String> test);

    /**
     *
     * @param name
     * @return the section helper factory for the giben name
     */
    SectionHelperFactory<?> getSectionHelperFactory(String name);

    /**
     *
     * @return an immutable map of section helper factories
     */
    Map<String, SectionHelperFactory<?>> getSectionHelperFactories();

    /**
     *
     * @return an immutable list of value resolvers
     */
    List<ValueResolver> getValueResolvers();

    /**
     *
     * @return an immutable list of namespace resolvers
     */
    List<NamespaceResolver> getNamespaceResolvers();

    /**
     *
     * @return the evaluator used to evaluate expressions
     */
    Evaluator getEvaluator();

    /**
     * @return an immutable list of template instance initializers
     */
    List<TemplateInstance.Initializer> getTemplateInstanceInitializers();

    /**
     * The global rendering timeout in milliseconds. It is used if no {@code timeout} instance attribute is set.
     *
     * @return the global rendering timeout
     * @see TemplateInstance#TIMEOUT
     */
    long getTimeout();

    /**
     *
     * @return {@code true} if the timeout should also used for asynchronous rendering methods
     */
    boolean useAsyncTimeout();

    /**
     * Locates the template with the given id.
     * <p>
     * All locators registered via {@link EngineBuilder#addLocator(TemplateLocator)} are used.
     *
     * @param id
     * @return the template location for the given id, or an empty {@link Optional} if no template was found
     * @see TemplateLocator#locate(String)
     */
    Optional<TemplateLocation> locate(String id);

    /**
     * @return {@code true} if the parser should remove standalone lines from the output, {@code false} otherwise
     */
    boolean removeStandaloneLines();

    /**
     * Initializes a new {@link EngineBuilder} instance from this engine.
     * <p>
     * The {@link EngineBuilder#iterationMetadataPrefix(String) is not set but if a
     * {@link io.quarkus.qute.LoopSectionHelper.Factory} is registered then the original prefix should be honored.
     *
     * @return a new builder instance initialized from this engine
     */
    EngineBuilder newBuilder();
}
