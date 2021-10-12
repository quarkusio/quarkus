package io.quarkus.qute.runtime;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.HtmlEscaper;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Resolver;
import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateLocator.TemplateLocation;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.ValueResolvers;
import io.quarkus.qute.Variant;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Startup;

@Startup(Interceptor.Priority.PLATFORM_BEFORE)
@Singleton
public class EngineProducer {

    public static final String INJECT_NAMESPACE = "inject";

    private static final String TAGS = "tags/";

    private static final Logger LOGGER = Logger.getLogger(EngineProducer.class);

    private final Engine engine;
    private final ContentTypes contentTypes;
    private final List<String> tags;
    private final List<String> suffixes;
    private final String basePath;
    private final String tagPath;
    private final Pattern templatePathExclude;

    public EngineProducer(QuteContext context, QuteConfig config, QuteRuntimeConfig runtimeConfig,
            Event<EngineBuilder> builderReady, Event<Engine> engineReady, ContentTypes contentTypes, LaunchMode launchMode) {
        this.contentTypes = contentTypes;
        this.suffixes = config.suffixes;
        this.basePath = "templates/";
        this.tagPath = basePath + TAGS;
        this.tags = context.getTags();
        this.templatePathExclude = config.templatePathExclude;

        LOGGER.debugf("Initializing Qute [templates: %s, tags: %s, resolvers: %s", context.getTemplatePaths(), tags,
                context.getResolverClasses());

        EngineBuilder builder = Engine.builder();

        // We don't register the map resolver because of param declaration validation
        // See DefaultTemplateExtensions
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
            // If needed use a specific result mapper for the selected strategy  
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
                // Throw an expection in the development mode
                if (launchMode == LaunchMode.DEVELOPMENT) {
                    builder.addResultMapper(new PropertyNotFoundThrowException());
                }
            }
        }

        // Escape some characters for HTML templates
        builder.addResultMapper(new HtmlEscaper());

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
        builder.addNamespaceResolver(NamespaceResolver.builder(INJECT_NAMESPACE).resolve(ctx -> {
            InstanceHandle<Object> bean = Arc.container().instance(ctx.getName());
            return bean.isAvailable() ? bean.get() : Results.NotFound.from(ctx);
        }).build());

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
            String tagName = tag.contains(".") ? tag.substring(0, tag.lastIndexOf('.')) : tag;
            String tagTemplateId = TAGS + tagName;
            LOGGER.debugf("Registered UserTagSectionHelper for %s [%s]", tagName, tagTemplateId);
            builder.addSectionHelper(new UserTagSectionHelper.Factory(tagName, tagTemplateId));
        }
        // Add locator
        builder.addLocator(this::locate);
        engine = builder.build();

        // Load discovered templates
        for (String path : context.getTemplatePaths()) {
            engine.getTemplate(path);
        }
        engineReady.fire(engine);
    }

    @Produces
    @ApplicationScoped
    Engine getEngine() {
        return engine;
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
            return Optional.of(new ResourceTemplateLocation(resource, guessVariant(templatePath)));
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

    Variant guessVariant(String path) {
        // TODO detect locale and encoding
        return Variant.forContentType(contentTypes.getContentType(path));
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
            try {
                return new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8);
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
