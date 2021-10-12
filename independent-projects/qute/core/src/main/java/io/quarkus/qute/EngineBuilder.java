package io.quarkus.qute;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder for {@link Engine}.
 */
public final class EngineBuilder {

    final Map<String, SectionHelperFactory<?>> sectionHelperFactories;
    final List<ValueResolver> valueResolvers;
    final List<NamespaceResolver> namespaceResolvers;
    final List<TemplateLocator> locators;
    final List<ResultMapper> resultMappers;
    Function<String, SectionHelperFactory<?>> sectionHelperFunc;
    final List<ParserHook> parserHooks;
    boolean removeStandaloneLines;
    boolean strictRendering;
    String iterationMetadataPrefix;

    EngineBuilder() {
        this.sectionHelperFactories = new HashMap<>();
        this.valueResolvers = new ArrayList<>();
        this.namespaceResolvers = new ArrayList<>();
        this.locators = new ArrayList<>();
        this.resultMappers = new ArrayList<>();
        this.parserHooks = new ArrayList<>();
        this.strictRendering = true;
        this.removeStandaloneLines = true;
        this.iterationMetadataPrefix = LoopSectionHelper.Factory.ITERATION_METADATA_PREFIX_ALIAS_UNDERSCORE;
    }

    public EngineBuilder addSectionHelper(SectionHelperFactory<?> factory) {
        for (String alias : factory.getDefaultAliases()) {
            sectionHelperFactories.put(alias, factory);
        }
        return this;
    }

    public EngineBuilder addSectionHelpers(SectionHelperFactory<?>... factories) {
        for (SectionHelperFactory<?> factory : factories) {
            addSectionHelper(factory);
        }
        return this;
    }

    public EngineBuilder addSectionHelper(String name, SectionHelperFactory<?> factory) {
        addSectionHelper(factory);
        sectionHelperFactories.put(name, factory);
        return this;
    }

    public EngineBuilder addDefaultSectionHelpers() {
        return addSectionHelpers(new IfSectionHelper.Factory(), new LoopSectionHelper.Factory(iterationMetadataPrefix),
                new WithSectionHelper.Factory(), new IncludeSectionHelper.Factory(), new InsertSectionHelper.Factory(),
                new SetSectionHelper.Factory(), new WhenSectionHelper.Factory(), new EvalSectionHelper.Factory());
    }

    public EngineBuilder addValueResolver(Supplier<ValueResolver> resolverSupplier) {
        return addValueResolver(resolverSupplier.get());
    }

    public EngineBuilder addValueResolvers(ValueResolver... resolvers) {
        for (ValueResolver valueResolver : resolvers) {
            addValueResolver(valueResolver);
        }
        return this;
    }

    public EngineBuilder addValueResolver(ValueResolver resolver) {
        this.valueResolvers.add(resolver);
        return this;
    }

    /**
     * Add the default value resolvers.
     * 
     * @return self
     */
    public EngineBuilder addDefaultValueResolvers() {
        return addValueResolvers(ValueResolvers.mapResolver(), ValueResolvers.mapperResolver(),
                ValueResolvers.mapEntryResolver(), ValueResolvers.collectionResolver(), ValueResolvers.listResolver(),
                ValueResolvers.thisResolver(), ValueResolvers.orResolver(), ValueResolvers.trueResolver(),
                ValueResolvers.logicalAndResolver(), ValueResolvers.logicalOrResolver(), ValueResolvers.orEmpty(),
                ValueResolvers.arrayResolver());
    }

    public EngineBuilder addDefaults() {
        return addDefaultSectionHelpers().addDefaultValueResolvers();
    }

    public EngineBuilder addNamespaceResolver(NamespaceResolver resolver) {
        String namespace = Namespaces.requireValid(resolver.getNamespace());
        for (NamespaceResolver nsResolver : namespaceResolvers) {
            if (nsResolver.getNamespace().equals(namespace)
                    && resolver.getPriority() == nsResolver.getPriority()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Namespace [%s] may not be handled by multiple resolvers of the same priority [%s]: %s and %s",
                                namespace, resolver.getPriority(), nsResolver, resolver));
            }
        }
        this.namespaceResolvers.add(resolver);
        return this;
    }

    /**
     * A {@link Reader} instance produced by a locator is immediately closed right after the template content is parsed.
     * 
     * @param locator
     * @return self
     * @see Engine#getTemplate(String)
     */
    public EngineBuilder addLocator(TemplateLocator locator) {
        this.locators.add(locator);
        return this;
    }

    public EngineBuilder addParserHook(ParserHook parserHook) {
        this.parserHooks.add(parserHook);
        return this;
    }

    /**
     * 
     * @param resultMapper
     * @return self
     */
    public EngineBuilder addResultMapper(ResultMapper mapper) {
        this.resultMappers.add(mapper);
        return this;
    }

    public EngineBuilder computeSectionHelper(Function<String, SectionHelperFactory<?>> func) {
        this.sectionHelperFunc = func;
        return this;
    }

    /**
     * Specify whether the parser should remove standalone lines from the output.
     * <p>
     * A standalone line is a line that contains at least one section tag, parameter declaration, or comment but no expression
     * and no non-whitespace character.
     * 
     * @param value
     * @return self
     */
    public EngineBuilder removeStandaloneLines(boolean value) {
        this.removeStandaloneLines = value;
        return this;
    }

    /**
     * If set to {@code true} then any expression that is evaluated to a {@link Results.NotFound} will always result in a
     * {@link TemplateException} and the rendering is aborted.
     * <p>
     * Strict rendering is enabled by default.
     * 
     * @param value
     * @return self
     */
    public EngineBuilder strictRendering(boolean value) {
        this.strictRendering = value;
        return this;
    }

    /**
     * This prefix is used to access the iteration metadata inside a loop section. This method must be called before a
     * {@link LoopSectionHelper.Factory} is registered, i.e. before {@link #addDefaultSectionHelpers()} or before
     * {@link #addSectionHelper(SectionHelperFactory)}.
     * <p>
     * A valid prefix consists of alphanumeric characters and underscores.
     * 
     * @param prefix
     * @return self
     * @see LoopSectionHelper.Factory
     */
    public EngineBuilder iterationMetadataPrefix(String prefix) {
        if (!LoopSectionHelper.Factory.ITERATION_METADATA_PREFIX_NONE.equals(prefix)
                && !LoopSectionHelper.Factory.ITERATION_METADATA_PREFIX_ALIAS_UNDERSCORE.equals(prefix)
                && !LoopSectionHelper.Factory.ITERATION_METADATA_PREFIX_ALIAS_QM.equals(prefix)
                && !Namespaces.NAMESPACE_PATTERN.matcher(prefix).matches()) {
            throw new TemplateException("[" + prefix
                    + "] is not a valid iteration metadata prefix. The value can only consist of alphanumeric characters and underscores.");
        }
        this.iterationMetadataPrefix = prefix;
        return this;
    }

    /**
     * 
     * @return a new engine instance
     */
    public Engine build() {
        return new EngineImpl(this);
    }

}
