package io.quarkus.qute;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

/**
 * Builder for {@link Engine}.
 * <p>
 * If using Qute "standalone" you'll need to create an instance of the {@link Engine} first:
 *
 * <pre>
 * Engine engine = Engine.builder()
 *         // add the default set of value resolvers and section helpers
 *         .addDefaults()
 *         .build();
 * </pre>
 *
 * This construct is not thread-safe and should not be reused.
 */
public final class EngineBuilder {

    private static final Logger LOG = Logger.getLogger(EngineBuilder.class);

    final Map<String, SectionHelperFactory<?>> sectionHelperFactories;
    final List<ValueResolver> valueResolvers;
    final List<NamespaceResolver> namespaceResolvers;
    final List<TemplateLocator> locators;
    final List<ResultMapper> resultMappers;
    final List<TemplateInstance.Initializer> initializers;
    Function<String, SectionHelperFactory<?>> sectionHelperFunc;
    final List<ParserHook> parserHooks;
    boolean removeStandaloneLines;
    boolean strictRendering;
    String iterationMetadataPrefix;
    long timeout;
    boolean useAsyncTimeout;
    final List<EngineListener> listeners;

    EngineBuilder() {
        this.sectionHelperFactories = new HashMap<>();
        this.valueResolvers = new ArrayList<>();
        this.namespaceResolvers = new ArrayList<>();
        this.locators = new ArrayList<>();
        this.resultMappers = new ArrayList<>();
        this.parserHooks = new ArrayList<>();
        this.initializers = new ArrayList<>();
        this.strictRendering = true;
        this.removeStandaloneLines = true;
        this.iterationMetadataPrefix = LoopSectionHelper.Factory.ITERATION_METADATA_PREFIX_ALIAS_UNDERSCORE;
        this.timeout = 10_000;
        this.useAsyncTimeout = true;
        this.listeners = new ArrayList<>();
    }

    /**
     * Register the factory for all default aliases.
     *
     * @param factory
     * @return self
     * @see SectionHelperFactory#getDefaultAliases()
     */
    public EngineBuilder addSectionHelper(SectionHelperFactory<?> factory) {
        factory = cachedFactory(factory);
        for (String alias : factory.getDefaultAliases()) {
            sectionHelperFactories.put(alias, factory);
        }
        return this;
    }

    /**
     * Register the factories for all default aliases.
     *
     * @param factory
     * @return self
     * @see SectionHelperFactory#getDefaultAliases()
     */
    public EngineBuilder addSectionHelpers(SectionHelperFactory<?>... factories) {
        for (SectionHelperFactory<?> factory : factories) {
            addSectionHelper(factory);
        }
        return this;
    }

    /**
     * Register the factory for all default aliases and the specified name.
     *
     * @param factory
     * @return self
     * @see SectionHelperFactory#getDefaultAliases()
     */
    public EngineBuilder addSectionHelper(String name, SectionHelperFactory<?> factory) {
        factory = cachedFactory(factory);
        addSectionHelper(factory);
        sectionHelperFactories.put(name, factory);
        return this;
    }

    public EngineBuilder addDefaultSectionHelpers() {
        return addSectionHelpers(new IfSectionHelper.Factory(), new LoopSectionHelper.Factory(iterationMetadataPrefix),
                new WithSectionHelper.Factory(), new IncludeSectionHelper.Factory(), new InsertSectionHelper.Factory(),
                new SetSectionHelper.Factory(), new WhenSectionHelper.Factory(), new EvalSectionHelper.Factory(),
                new FragmentSectionHelper.Factory());
    }

    /**
     *
     * @param resolverSupplier
     * @return self
     * @see EngineListener
     */
    public EngineBuilder addValueResolver(Supplier<ValueResolver> resolverSupplier) {
        return addValueResolver(resolverSupplier.get());
    }

    /**
     *
     * @param resolvers
     * @return self
     * @see EngineListener
     */
    public EngineBuilder addValueResolvers(ValueResolver... resolvers) {
        for (ValueResolver valueResolver : resolvers) {
            addValueResolver(valueResolver);
        }
        return this;
    }

    /**
     *
     * @param resolver
     * @return self
     * @see EngineListener
     */
    public EngineBuilder addValueResolver(ValueResolver resolver) {
        this.valueResolvers.add(resolver);
        return addListener(resolver);
    }

    /**
     * Add the default set of value resolvers.
     *
     * @return self
     * @see #addValueResolver(ValueResolver)
     */
    public EngineBuilder addDefaultValueResolvers() {
        return addValueResolvers(ValueResolvers.mapResolver(), ValueResolvers.mapperResolver(),
                ValueResolvers.mapEntryResolver(), ValueResolvers.collectionResolver(), ValueResolvers.listResolver(),
                ValueResolvers.thisResolver(), ValueResolvers.orResolver(), ValueResolvers.trueResolver(),
                ValueResolvers.logicalAndResolver(), ValueResolvers.logicalOrResolver(), ValueResolvers.orEmpty(),
                ValueResolvers.arrayResolver(), ValueResolvers.plusResolver(), ValueResolvers.minusResolver(),
                ValueResolvers.modResolver(), ValueResolvers.numberValueResolver(), ValueResolvers.equalsResolver());
    }

    /**
     * Add the default set of value resolvers and section helpers.
     *
     * @return self
     * @see #addValueResolver(ValueResolver)
     * @see #addSectionHelper(SectionHelperFactory)
     */
    public EngineBuilder addDefaults() {
        return addDefaultSectionHelpers().addDefaultValueResolvers();
    }

    /**
     *
     * @param resolver
     * @return self
     * @throws IllegalArgumentException if there is a resolver of the same priority for the given namespace
     * @see EngineListener
     */
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
        return addListener(resolver);
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

    /**
     *
     * @param parserHook
     * @return self
     * @see ParserHelper
     */
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

    /**
     *
     * @param initializer
     * @return self
     */
    public EngineBuilder addTemplateInstanceInitializer(TemplateInstance.Initializer initializer) {
        this.initializers.add(initializer);
        return this;
    }

    /**
     * The function is used if no section helper registered via {@link #addSectionHelper(SectionHelperFactory)} matches a
     * section name.
     *
     * @param func
     * @return self
     */
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
     * <p>
     * Keep in mind that the prefix must be set before the {@link LoopSectionHelper.Factory} is registered, for example before
     * the {@link #addDefaultSectionHelpers()} method is called. In other words, the {@link LoopSectionHelper.Factory} must be
     * re-registered after the prefix is set.
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
     * The global rendering timeout.
     *
     * @param value Timeout in milliseconds
     * @return self
     */
    public EngineBuilder timeout(long value) {
        this.timeout = value;
        return this;
    }

    /**
     * If set to {@code true} the timeout (either global or set via the {@code timeout} instance attribute) is also used for
     * asynchronous rendering methods, such as {@link TemplateInstance#createUni()} and {@link TemplateInstance#renderAsync()}.
     *
     * @param value
     * @return self
     */
    public EngineBuilder useAsyncTimeout(boolean value) {
        this.useAsyncTimeout = value;
        return this;
    }

    /**
     * Value and namespace resolvers that also implement {@link EngineListener} are registered automatically.
     *
     * @param listener
     * @return self
     */
    public EngineBuilder addEngineListener(EngineListener listener) {
        this.listeners.add(Objects.requireNonNull(listener));
        return this;
    }

    /**
     *
     * @return a new engine instance
     */
    public Engine build() {
        EngineImpl engine = new EngineImpl(this);
        for (EngineListener listener : listeners) {
            try {
                listener.engineBuilt(engine);
            } catch (Throwable e) {
                LOG.warnf("Engine listener error: " + e);
            }
        }
        return engine;
    }

    private SectionHelperFactory<?> cachedFactory(SectionHelperFactory<?> factory) {
        if (factory instanceof CachedConfigSectionHelperFactory || !factory.cacheFactoryConfig()) {
            return factory;
        }
        return new CachedConfigSectionHelperFactory<>(factory);
    }

    private EngineBuilder addListener(Object obj) {
        if (obj instanceof EngineListener listener) {
            listeners.add(listener);
        }
        return this;
    }

    /**
     * Receives notifications about Engine lifecycle.
     * <p>
     * Value and namespace resolvers that also implement {@link EngineListener} are registered automatically.
     */
    public interface EngineListener {

        default void engineBuilt(Engine engine) {
        }

    }

    static class CachedConfigSectionHelperFactory<T extends SectionHelper> implements SectionHelperFactory<T> {

        private final SectionHelperFactory<T> delegate;
        private final List<String> defaultAliases;
        private final ParametersInfo parameters;
        private final List<String> blockLabels;

        public CachedConfigSectionHelperFactory(SectionHelperFactory<T> delegate) {
            this.delegate = delegate;
            this.defaultAliases = delegate.getDefaultAliases();
            this.parameters = delegate.getParameters();
            this.blockLabels = delegate.getBlockLabels();
        }

        @Override
        public List<String> getDefaultAliases() {
            return defaultAliases;
        }

        @Override
        public ParametersInfo getParameters() {
            return parameters;
        }

        @Override
        public List<String> getBlockLabels() {
            return blockLabels;
        }

        @Override
        public boolean cacheFactoryConfig() {
            return true;
        }

        @Override
        public T initialize(SectionInitContext context) {
            return delegate.initialize(context);
        }

        @Override
        public boolean treatUnknownSectionsAsBlocks() {
            return delegate.treatUnknownSectionsAsBlocks();
        }

        @Override
        public Scope initializeBlock(Scope outerScope, BlockInfo block) {
            return delegate.initializeBlock(outerScope, block);
        }

        @Override
        public MissingEndTagStrategy missingEndTagStrategy() {
            return delegate.missingEndTagStrategy();
        }

    }

}
