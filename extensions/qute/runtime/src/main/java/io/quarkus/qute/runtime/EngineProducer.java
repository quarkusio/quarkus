package io.quarkus.qute.runtime;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

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
import io.quarkus.qute.Escaper;
import io.quarkus.qute.Expression;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.RawString;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.ResultMapper;
import io.quarkus.qute.Results.Result;
import io.quarkus.qute.TemplateLocator.TemplateLocation;
import io.quarkus.qute.TemplateNode.Origin;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.ValueResolvers;
import io.quarkus.qute.Variant;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.runtime.Startup;

@Startup(Interceptor.Priority.PLATFORM_BEFORE)
@Singleton
public class EngineProducer {

    public static final String INJECT_NAMESPACE = "inject";

    private static final Logger LOGGER = Logger.getLogger(EngineProducer.class);

    private final Engine engine;
    private final List<String> tags;
    private final List<String> suffixes;
    private final String basePath;
    private final String tagPath;

    public EngineProducer(QuteContext context, Event<EngineBuilder> event) {
        this.suffixes = context.getConfig().suffixes;
        this.basePath = "templates/";
        this.tagPath = basePath + "tags/";
        this.tags = context.getTags();

        LOGGER.debugf("Initializing Qute [templates: %s, tags: %s, resolvers: %s", context.getTemplatePaths(), tags,
                context.getResolverClasses());

        EngineBuilder builder = Engine.builder()
                .addDefaultSectionHelpers();

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

        // Escape some characters for HTML templates
        Escaper htmlEscaper = Escaper.builder().add('"', "&quot;").add('\'', "&#39;")
                .add('&', "&amp;").add('<', "&lt;").add('>', "&gt;").build();
        builder.addResultMapper(new ResultMapper() {

            @Override
            public boolean appliesTo(Origin origin, Object result) {
                return !(result instanceof RawString)
                        && origin.getVariant().filter(EngineProducer::requiresDefaultEscaping).isPresent();
            }

            @Override
            public String map(Object result, Expression expression) {
                return htmlEscaper.escape(result.toString());
            }
        });

        // Fallback reflection resolver
        builder.addValueResolver(new ReflectionValueResolver());

        // Allow anyone to customize the builder
        event.fire(builder);

        // Resolve @Named beans
        builder.addNamespaceResolver(NamespaceResolver.builder(INJECT_NAMESPACE).resolve(ctx -> {
            InstanceHandle<Object> bean = Arc.container().instance(ctx.getName());
            return bean.isAvailable() ? bean.get() : Result.NOT_FOUND;
        }).build());

        // Add generated resolvers
        for (String resolverClass : context.getResolverClasses()) {
            builder.addValueResolver(createResolver(resolverClass));
            LOGGER.debugf("Added generated value resolver: %s", resolverClass);
        }
        // Add tags
        for (String tag : tags) {
            // Strip suffix, item.html -> item
            String tagName = tag.contains(".") ? tag.substring(0, tag.lastIndexOf('.')) : tag;
            LOGGER.debugf("Registered UserTagSectionHelper for %s", tagName);
            builder.addSectionHelper(new UserTagSectionHelper.Factory(tagName));
        }
        // Add locator
        builder.addLocator(this::locate);
        engine = builder.build();

        // Load discovered templates
        for (String path : context.getTemplatePaths()) {
            engine.getTemplate(path);
        }
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

    List<String> getSuffixes() {
        return suffixes;
    }

    private ValueResolver createResolver(String resolverClassName) {
        try {
            Class<?> resolverClazz = Thread.currentThread()
                    .getContextClassLoader().loadClass(resolverClassName);
            if (ValueResolver.class.isAssignableFrom(resolverClazz)) {
                return (ValueResolver) resolverClazz.newInstance();
            }
            throw new IllegalStateException("Not a value resolver: " + resolverClassName);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to create resolver: " + resolverClassName, e);
        }
    }

    /**
     * @param path
     * @return the optional reader
     */
    private Optional<TemplateLocation> locate(String path) {
        URL resource = null;
        // First try to locate a tag template
        if (tags.stream().anyMatch(tag -> tag.startsWith(path))) {
            LOGGER.debugf("Locate tag for %s", path);
            resource = locatePath(tagPath + path);
            // Try path with suffixes
            for (String suffix : suffixes) {
                resource = locatePath(tagPath + path + "." + suffix);
                if (resource != null) {
                    break;
                }
            }
        }
        if (resource == null) {
            String templatePath = basePath + path;
            LOGGER.debugf("Locate template for %s", templatePath);
            resource = locatePath(templatePath);
        }
        if (resource != null) {
            return Optional.of(new ResourceTemplateLocation(resource, guessVariant(path)));
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

    static Variant guessVariant(String path) {
        // TODO we need a proper way to detect the variant
        int suffixIdx = path.lastIndexOf('.');
        if (suffixIdx != -1) {
            String suffix = path.substring(suffixIdx);
            return new Variant(null, VariantTemplateProducer.parseMediaType(suffix), null);
        }
        return null;
    }

    static boolean requiresDefaultEscaping(Variant variant) {
        return variant.mediaType != null
                ? (Variant.TEXT_HTML.equals(variant.mediaType) || Variant.TEXT_XML.equals(variant.mediaType))
                : false;
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
                return new InputStreamReader(resource.openStream(), Charset.forName("utf-8"));
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
