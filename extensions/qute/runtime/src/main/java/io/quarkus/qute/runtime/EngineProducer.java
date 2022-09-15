package io.quarkus.qute.runtime;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.jboss.logging.Logger;

import io.quarkus.arc.All;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Expression;
import io.quarkus.qute.HtmlEscaper;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.Qute;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Resolver;
import io.quarkus.qute.Results;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateInstance.Initializer;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.TemplateLocator.TemplateLocation;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.ValueResolvers;
import io.quarkus.qute.Variant;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;

@Startup(Interceptor.Priority.PLATFORM_BEFORE)
@Singleton
public class EngineProducer {

    public static final String INJECT_NAMESPACE = "inject";
    public static final String CDI_NAMESPACE = "cdi";
    public static final String DEPENDENT_INSTANCES = "q_dep_inst";

    private static final String TAGS = "tags/";

    private static final Logger LOGGER = Logger.getLogger(EngineProducer.class);

    private final Engine engine;
    private final ContentTypes contentTypes;
    private final List<String> tags;
    private final List<String> suffixes;
    private final String basePath;
    private final String tagPath;
    private final Pattern templatePathExclude;
    private final Locale defaultLocale;
    private final Charset defaultCharset;
    private final ArcContainer container;

    public EngineProducer(QuteContext context, QuteConfig config, QuteRuntimeConfig runtimeConfig,
            Event<EngineBuilder> builderReady, Event<Engine> engineReady, ContentTypes contentTypes,
            LaunchMode launchMode, LocalesBuildTimeConfig locales, @All List<TemplateLocator> locators) {
        this.contentTypes = contentTypes;
        this.suffixes = config.suffixes;
        this.basePath = "templates/";
        this.tagPath = basePath + TAGS;
        this.tags = context.getTags();
        this.templatePathExclude = config.templatePathExclude;
        this.defaultLocale = locales.defaultLocale;
        this.defaultCharset = config.defaultCharset;
        this.container = Arc.container();

        LOGGER.debugf("Initializing Qute [templates: %s, tags: %s, resolvers: %s", context.getTemplatePaths(), tags,
                context.getResolverClasses());

        EngineBuilder builder = Engine.builder();

        // We don't register the map resolver because of param declaration validation
        builder.addValueResolver(ValueResolvers.thisResolver());
        builder.addValueResolver(ValueResolvers.orResolver());
        builder.addValueResolver(ValueResolvers.trueResolver());
        builder.addValueResolver(ValueResolvers.collectionResolver());
        builder.addValueResolver(ValueResolvers.mapperResolver());
        builder.addValueResolver(ValueResolvers.mapEntryResolver());
        // foo.string.raw returns a RawString which is never escaped
        builder.addValueResolver(ValueResolvers.rawResolver());
        builder.addValueResolver(ValueResolvers.logicalAndResolver());
        builder.addValueResolver(ValueResolvers.logicalOrResolver());
        builder.addValueResolver(ValueResolvers.orEmpty());
        // Note that arrays are handled specifically during validation
        builder.addValueResolver(ValueResolvers.arrayResolver());

        // Enable/disable strict rendering
        if (runtimeConfig.strictRendering) {
            builder.strictRendering(true);
        } else {
            builder.strictRendering(false);
            // If needed, use a specific result mapper for the selected strategy
            if (runtimeConfig.propertyNotFoundStrategy.isPresent()) {
                switch (runtimeConfig.propertyNotFoundStrategy.get()) {
                    case THROW_EXCEPTION:
                        builder.addResultMapper(new PropertyNotFoundThrowException());
                        break;
                    case NOOP:
                        builder.addResultMapper(new PropertyNotFoundNoop());
                        break;
                    case OUTPUT_ORIGINAL:
                        builder.addResultMapper(new PropertyNotFoundOutputOriginal());
                        break;
                    default:
                        // Use the default strategy
                        break;
                }
            } else {
                // Throw an exception in the development mode
                if (launchMode == LaunchMode.DEVELOPMENT) {
                    builder.addResultMapper(new PropertyNotFoundThrowException());
                }
            }
        }

        // Escape some characters for HTML/XML templates
        builder.addResultMapper(new HtmlEscaper(List.copyOf(config.escapeContentTypes)));

        // Fallback reflection resolver
        builder.addValueResolver(new ReflectionValueResolver());

        // Remove standalone lines if desired
        builder.removeStandaloneLines(runtimeConfig.removeStandaloneLines);

        // Iteration metadata prefix
        builder.iterationMetadataPrefix(config.iterationMetadataPrefix);

        // Default section helpers
        builder.addDefaultSectionHelpers();

        // Allow anyone to customize the builder
        builderReady.fire(builder);

        // Resolve @Named beans
        builder.addNamespaceResolver(NamespaceResolver.builder(INJECT_NAMESPACE).resolve(this::resolveInject).build());
        builder.addNamespaceResolver(NamespaceResolver.builder(CDI_NAMESPACE).resolve(this::resolveInject).build());

        // Add generated resolvers
        for (String resolverClass : context.getResolverClasses()) {
            Resolver resolver = createResolver(resolverClass);
            if (resolver instanceof NamespaceResolver) {
                builder.addNamespaceResolver((NamespaceResolver) resolver);
            } else {
                builder.addValueResolver((ValueResolver) resolver);
            }
            LOGGER.debugf("Added generated value resolver: %s", resolverClass);
        }
        // Add tags
        for (String tag : tags) {
            // Strip suffix, item.html -> item
            String tagName = tag.contains(".") ? tag.substring(0, tag.indexOf('.')) : tag;
            String tagTemplateId = TAGS + tagName;
            LOGGER.debugf("Registered UserTagSectionHelper for %s [%s]", tagName, tagTemplateId);
            builder.addSectionHelper(new UserTagSectionHelper.Factory(tagName, tagTemplateId));
        }
        // Add locator
        builder.addLocator(this::locate);
        registerCustomLocators(builder, locators);

        // Add a special parser hook for Qute.fmt() methods
        builder.addParserHook(new Qute.IndexedArgumentsParserHook());

        // Add template initializers
        for (String initializerClass : context.getTemplateInstanceInitializerClasses()) {
            builder.addTemplateInstanceInitializer(createInitializer(initializerClass));
        }

        // Add a special initializer for templates that contain an inject/cdi namespace expressions
        Map<String, Boolean> discoveredInjectTemplates = new HashMap<>();
        builder.addTemplateInstanceInitializer(new Initializer() {

            @Override
            public void accept(TemplateInstance instance) {
                Boolean hasInject = discoveredInjectTemplates.get(instance.getTemplate().getGeneratedId());
                if (hasInject == null) {
                    hasInject = hasInjectExpression(instance.getTemplate());
                }
                if (hasInject) {
                    // Add dependent beans map if the template contains a cdi namespace expression
                    instance.setAttribute(DEPENDENT_INSTANCES, new ConcurrentHashMap<>());
                    // Add a close action to destroy all dependent beans
                    instance.onRendered(new Runnable() {
                        @Override
                        public void run() {
                            Object dependentInstances = instance.getAttribute(EngineProducer.DEPENDENT_INSTANCES);
                            if (dependentInstances != null) {
                                @SuppressWarnings("unchecked")
                                ConcurrentMap<String, InstanceHandle<?>> existing = (ConcurrentMap<String, InstanceHandle<?>>) dependentInstances;
                                if (!existing.isEmpty()) {
                                    for (InstanceHandle<?> handle : existing.values()) {
                                        handle.close();
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });

        builder.timeout(runtimeConfig.timeout);
        builder.useAsyncTimeout(runtimeConfig.useAsyncTimeout);

        engine = builder.build();

        // Load discovered template files
        Map<String, List<Template>> discovered = new HashMap<>();
        for (String path : context.getTemplatePaths()) {
            Template template = engine.getTemplate(path);
            if (template != null) {
                for (String suffix : config.suffixes) {
                    if (path.endsWith(suffix)) {
                        String pathNoSuffix = path.substring(0, path.length() - (suffix.length() + 1));
                        List<Template> templates = discovered.get(pathNoSuffix);
                        if (templates == null) {
                            templates = new ArrayList<>();
                            discovered.put(pathNoSuffix, templates);
                        }
                        templates.add(template);
                        break;
                    }
                }
                discoveredInjectTemplates.put(template.getGeneratedId(), hasInjectExpression(template));
            }
        }
        // If it's a default suffix then register a path without suffix as well
        // hello.html -> hello, hello.html
        for (Entry<String, List<Template>> e : discovered.entrySet()) {
            processDefaultTemplate(e.getKey(), e.getValue(), config, engine);
        }

        engineReady.fire(engine);

        // Set the engine instance
        Qute.setEngine(engine);
    }

    private void registerCustomLocators(EngineBuilder builder,
            List<TemplateLocator> locators) {
        if (locators != null && !locators.isEmpty()) {
            for (TemplateLocator locator : locators) {
                builder.addLocator(locator);
            }
        }
    }

    @Produces
    @ApplicationScoped
    Engine getEngine() {
        return engine;
    }

    void onShutdown(@Observes ShutdownEvent event) {
        // Make sure to clear the Qute cache
        Qute.clearCache();
    }

    String getBasePath() {
        return basePath;
    }

    String getTagPath() {
        return tagPath;
    }

    private Resolver createResolver(String resolverClassName) {
        try {
            Class<?> resolverClazz = Thread.currentThread()
                    .getContextClassLoader().loadClass(resolverClassName);
            if (Resolver.class.isAssignableFrom(resolverClazz)) {
                return (Resolver) resolverClazz.getDeclaredConstructor().newInstance();
            }
            throw new IllegalStateException("Not a resolver: " + resolverClassName);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException("Unable to create resolver: " + resolverClassName, e);
        }
    }

    private TemplateInstance.Initializer createInitializer(String initializerClassName) {
        try {
            Class<?> initializerClazz = Thread.currentThread()
                    .getContextClassLoader().loadClass(initializerClassName);
            if (TemplateInstance.Initializer.class.isAssignableFrom(initializerClazz)) {
                return (TemplateInstance.Initializer) initializerClazz.getDeclaredConstructor().newInstance();
            }
            throw new IllegalStateException("Not an initializer: " + initializerClazz);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException("Unable to create initializer: " + initializerClassName, e);
        }
    }

    private Optional<TemplateLocation> locate(String path) {
        URL resource = null;
        String templatePath = basePath + path;
        LOGGER.debugf("Locate template for %s", templatePath);
        if (templatePathExclude.matcher(path).matches()) {
            return Optional.empty();
        }
        resource = locatePath(templatePath);
        if (resource == null) {
            // Try path with suffixes
            for (String suffix : suffixes) {
                String pathWithSuffix = path + "." + suffix;
                if (templatePathExclude.matcher(pathWithSuffix).matches()) {
                    continue;
                }
                templatePath = basePath + pathWithSuffix;
                resource = locatePath(templatePath);
                if (resource != null) {
                    break;
                }
            }
        }
        if (resource != null) {
            return Optional.of(new ResourceTemplateLocation(resource, createVariant(templatePath)));
        }
        return Optional.empty();
    }

    private URL locatePath(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = EngineProducer.class.getClassLoader();
        }
        return cl.getResource(path);
    }

    Variant createVariant(String path) {
        // Guess the content type from the path
        String contentType = contentTypes.getContentType(path);
        return new Variant(defaultLocale, defaultCharset, contentType);
    }

    private Object resolveInject(EvalContext ctx) {
        InjectableBean<?> bean = container.namedBean(ctx.getName());
        if (bean != null) {
            if (bean.getScope().equals(Dependent.class)) {
                // Dependent beans are shared across all expressions in a template for a single rendering operation
                Object dependentInstances = ctx.getAttribute(EngineProducer.DEPENDENT_INSTANCES);
                if (dependentInstances != null) {
                    @SuppressWarnings("unchecked")
                    ConcurrentMap<String, InstanceHandle<?>> existing = (ConcurrentMap<String, InstanceHandle<?>>) dependentInstances;
                    return existing.computeIfAbsent(ctx.getName(), name -> container.instance(bean)).get();
                }
            }
            return container.instance(bean).get();
        }
        return Results.NotFound.from(ctx);
    }

    private boolean hasInjectExpression(Template template) {
        for (Expression expression : template.getExpressions()) {
            if (isInjectExpression(expression)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInjectExpression(Expression expression) {
        String namespace = expression.getNamespace();
        if (namespace != null && (CDI_NAMESPACE.equals(namespace) || INJECT_NAMESPACE.equals(namespace))) {
            return true;
        }
        for (Expression.Part part : expression.getParts()) {
            if (part.isVirtualMethod()) {
                for (Expression param : part.asVirtualMethod().getParameters()) {
                    if (param.isLiteral()) {
                        continue;
                    }
                    if (isInjectExpression(param)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void processDefaultTemplate(String path, List<Template> templates, QuteConfig config, Engine engine) {
        if (engine.isTemplateLoaded(path)) {
            return;
        }
        for (String suffix : config.suffixes) {
            for (Template template : templates) {
                if (template.getId().endsWith(suffix)) {
                    engine.putTemplate(path, template);
                    return;
                }
            }
        }
    }

    static class ResourceTemplateLocation implements TemplateLocation {

        private final URL resource;
        private final Optional<Variant> variant;

        public ResourceTemplateLocation(URL resource, Variant variant) {
            this.resource = resource;
            this.variant = Optional.ofNullable(variant);
        }

        @Override
        public Reader read() {
            Charset charset = null;
            if (variant.isPresent()) {
                charset = variant.get().getCharset();
            }
            if (charset == null) {
                charset = StandardCharsets.UTF_8;
            }
            try {
                return new InputStreamReader(resource.openStream(), charset);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public Optional<Variant> getVariant() {
            return variant;
        }

    }

}
