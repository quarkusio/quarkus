package io.quarkus.qute;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Template engine configuration.
 */
public interface Engine {

    static EngineBuilder builder() {
        return new EngineBuilder();
    }

    default Template parse(String content) {
        return parse(content, null);
    }

    public Template parse(String content, Variant variant);

    public SectionHelperFactory<?> getSectionHelperFactory(String name);

    public Map<String, SectionHelperFactory<?>> getSectionHelperFactories();

    public List<ValueResolver> getValueResolvers();

    public List<NamespaceResolver> getNamespaceResolvers();

    public Evaluator getEvaluator();

    /**
     * 
     * @return an immutable list of result mappers
     */
    public List<ResultMapper> getResultMappers();

    /**
     *
     * @param id
     * @param template
     * @return the previous value or null
     */
    public Template putTemplate(String id, Template template);

    /**
     * Obtain a compiled template for the given id. The template could be registered using
     * {@link #putTemplate(String, Template)} or loaded by a template locator.
     * 
     * @param id
     * @return the template or null
     * @see EngineBuilder#addLocator(java.util.function.Function)
     */
    public Template getTemplate(String id);

    /**
     * Removes all templates from the cache.
     */
    public void clearTemplates();

    /**
     * Removes the templates for which the mapping id matches the given predicate.
     * 
     * @param test
     */
    public void removeTemplates(Predicate<String> test);

}
