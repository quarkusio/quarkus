package io.quarkus.qute;

import io.quarkus.qute.TemplateLocator.TemplateLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.jboss.logging.Logger;

/**
 * 
 */
class EngineImpl implements Engine {

    private static final Logger LOGGER = Logger.getLogger(EngineImpl.class);

    private final Map<String, SectionHelperFactory<?>> sectionHelperFactories;
    private final Function<String, SectionHelperFactory<?>> sectionHelperFunc;
    private final List<ValueResolver> valueResolvers;
    private final List<NamespaceResolver> namespaceResolvers;
    private final Evaluator evaluator;
    private final Map<String, Template> templates;
    private final List<TemplateLocator> locators;
    private final List<ResultMapper> resultMappers;
    private final PublisherFactory publisherFactory;
    private final AtomicLong idGenerator = new AtomicLong(0);

    EngineImpl(Map<String, SectionHelperFactory<?>> sectionHelperFactories, List<ValueResolver> valueResolvers,
            List<NamespaceResolver> namespaceResolvers, List<TemplateLocator> locators,
            List<ResultMapper> resultMappers, Function<String, SectionHelperFactory<?>> sectionHelperFunc) {
        this.sectionHelperFactories = new HashMap<>(sectionHelperFactories);
        this.valueResolvers = sort(valueResolvers);
        this.namespaceResolvers = ImmutableList.copyOf(namespaceResolvers);
        this.evaluator = new EvaluatorImpl(this.valueResolvers);
        this.templates = new ConcurrentHashMap<>();
        this.locators = sort(locators);
        ServiceLoader<PublisherFactory> loader = ServiceLoader.load(PublisherFactory.class);
        Iterator<PublisherFactory> iterator = loader.iterator();
        if (iterator.hasNext()) {
            this.publisherFactory = iterator.next();
        } else {
            this.publisherFactory = null;
        }
        if (iterator.hasNext()) {
            throw new IllegalStateException(
                    "Multiple reactive factories found: " + StreamSupport.stream(loader.spliterator(), false)
                            .map(Object::getClass).map(Class::getName).collect(Collectors.joining(",")));
        }
        this.resultMappers = sort(resultMappers);
        this.sectionHelperFunc = sectionHelperFunc;
    }

    @Override
    public Template parse(String content, Variant variant) {
        String generatedId = generateId();
        return new Parser(this).parse(new StringReader(content), Optional.ofNullable(variant), generatedId, generatedId);
    }

    @Override
    public SectionHelperFactory<?> getSectionHelperFactory(String name) {
        SectionHelperFactory<?> factory = sectionHelperFactories.get(name);
        if (factory != null) {
            return factory;
        }
        return sectionHelperFunc != null ? sectionHelperFunc.apply(name) : null;
    }

    public Map<String, SectionHelperFactory<?>> getSectionHelperFactories() {
        return Collections.unmodifiableMap(sectionHelperFactories);
    }

    public List<ValueResolver> getValueResolvers() {
        return valueResolvers;
    }

    public List<NamespaceResolver> getNamespaceResolvers() {
        return namespaceResolvers;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public List<ResultMapper> getResultMappers() {
        return resultMappers;
    }

    public Template putTemplate(String id, Template template) {
        return templates.put(id, template);
    }

    public Template getTemplate(String id) {
        return templates.computeIfAbsent(id, this::load);
    }

    @Override
    public void clearTemplates() {
        templates.clear();
    }

    @Override
    public void removeTemplates(Predicate<String> test) {
        templates.keySet().removeIf(test);
    }

    PublisherFactory getPublisherFactory() {
        return publisherFactory;
    }

    String generateId() {
        return "" + idGenerator.incrementAndGet();
    }

    private Template load(String id) {
        for (TemplateLocator locator : locators) {
            Optional<TemplateLocation> location = locator.locate(id);
            if (location.isPresent()) {
                try (Reader r = location.get().read()) {
                    return new Parser(this).parse(ensureBufferedReader(r), location.get().getVariant(), id, generateId());
                } catch (IOException e) {
                    LOGGER.warn("Unable to close the reader for " + id, e);
                }
            }
        }
        return null;
    }

    private static <T extends WithPriority> List<T> sort(Collection<T> items) {
        List<T> sorted = new ArrayList<>(items);
        // Higher priority wins
        sorted.sort(Comparator.comparingInt(WithPriority::getPriority).reversed());
        return ImmutableList.copyOf(sorted);
    }

    private Reader ensureBufferedReader(Reader reader) {
        return reader instanceof BufferedReader ? reader
                : new BufferedReader(
                        reader);
    }

}
