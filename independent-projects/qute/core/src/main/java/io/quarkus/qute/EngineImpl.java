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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jboss.logging.Logger;

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
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final List<ParserHook> parserHooks;
    final boolean removeStandaloneLines;

    EngineImpl(EngineBuilder builder) {
        this.sectionHelperFactories = Collections.unmodifiableMap(new HashMap<>(builder.sectionHelperFactories));
        this.valueResolvers = sort(builder.valueResolvers);
        this.namespaceResolvers = ImmutableList.<NamespaceResolver> builder()
                .addAll(builder.namespaceResolvers).add(new TemplateImpl.DataNamespaceResolver()).build();
        this.evaluator = new EvaluatorImpl(this.valueResolvers, this.namespaceResolvers, builder.strictRendering);
        this.templates = new ConcurrentHashMap<>();
        this.locators = sort(builder.locators);
        this.resultMappers = sort(builder.resultMappers);
        this.sectionHelperFunc = builder.sectionHelperFunc;
        this.parserHooks = ImmutableList.copyOf(builder.parserHooks);
        this.removeStandaloneLines = builder.removeStandaloneLines;
    }

    @Override
    public Template parse(String content, Variant variant, String id) {
        String generatedId = generateId();
        return newParser(id != null ? id : generatedId, new StringReader(content), Optional.ofNullable(variant), generatedId)
                .parse();
    }

    private Parser newParser(String id, Reader reader, Optional<Variant> variant, String generatedId) {
        Parser parser = new Parser(this, reader, id, generatedId, variant);
        for (ParserHook parserHook : parserHooks) {
            parserHook.beforeParsing(parser);
        }
        return parser;
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
        return sectionHelperFactories;
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

    @Override
    public String mapResult(Object result, Expression expression) {
        String val = null;
        for (ResultMapper mapper : resultMappers) {
            if (mapper.appliesTo(expression.getOrigin(), result)) {
                val = mapper.map(result, expression);
                break;
            }
        }
        if (val == null) {
            val = result.toString();
        }
        return val;
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

    String generateId() {
        return "" + idGenerator.incrementAndGet();
    }

    private Template load(String id) {
        for (TemplateLocator locator : locators) {
            Optional<TemplateLocation> location = locator.locate(id);
            if (location.isPresent()) {
                try (Reader r = location.get().read()) {
                    return newParser(id, ensureBufferedReader(r), location.get().getVariant(), generateId()).parse();
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
