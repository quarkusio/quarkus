package io.quarkus.qute.runtime;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Results.Result;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.ValueResolvers;

@Singleton
public class EngineProducer {

    public static final String INJECT_NAMESPACE = "inject";

    private static final Logger LOGGER = LoggerFactory.getLogger(EngineProducer.class);

    @Inject
    Event<EngineBuilder> event;

    private Engine engine;
    private List<String> tags;

    private List<String> suffixes;
    private String basePath;
    private String tagPath;

    void init(QuteConfig config, List<String> resolverClasses, List<String> templatePaths, List<String> tags) {
        if (engine != null) {
            LOGGER.warn("Qute already initialized!");
            return;
        }
        LOGGER.debug("Initializing Qute with: {}", resolverClasses);

        suffixes = config.suffixes;
        basePath = "META-INF/resources/" + (config.basePath.endsWith("/") ? config.basePath : config.basePath + "/");
        tagPath = basePath + "tags/";

        EngineBuilder builder = Engine.builder()
                .addDefaultSectionHelpers();

        // We don't register the map resolver because of param declaration validation
        // See DefaultTemplateExtensions
        builder.addValueResolvers(ValueResolvers.thisResolver(), ValueResolvers.orResolver(),
                ValueResolvers.collectionResolver(), ValueResolvers.mapperResolver(), ValueResolvers.mapEntryResolver());

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
        for (String resolverClass : resolverClasses) {
            builder.addValueResolver(createResolver(resolverClass));
            LOGGER.debug("Added generated value resolver: {}", resolverClass);
        }
        // Add tags
        this.tags = tags;
        for (String tag : tags) {
            // Strip suffix, item.html -> item
            String tagName = tag.contains(".") ? tag.substring(0, tag.lastIndexOf('.')) : tag;
            LOGGER.debug("Registered UserTagSectionHelper for {}", tagName);
            builder.addSectionHelper(new UserTagSectionHelper.Factory(tagName));
        }
        // Add locator
        builder.addLocator(this::locate);
        engine = builder.build();

        // Load discovered templates
        for (String path : templatePaths) {
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
    private Optional<Reader> locate(String path) {
        InputStream in = null;
        // First try to locate a tag template
        if (tags.stream().anyMatch(tag -> tag.startsWith(path))) {
            LOGGER.debug("Locate tag for {}", path);
            in = locatePath(tagPath + path);
            // Try path with suffixes
            for (String suffix : suffixes) {
                in = locatePath(tagPath + path + "." + suffix);
                if (in != null) {
                    break;
                }
            }
        }
        if (in == null) {
            String templatePath = basePath + path;
            LOGGER.debug("Locate template for {}", templatePath);
            in = locatePath(templatePath);
        }
        if (in != null) {
            return Optional.of(new InputStreamReader(in, Charset.forName("utf-8")));
        }
        return Optional.empty();
    }

    private InputStream locatePath(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = EngineProducer.class.getClassLoader();
        }
        return cl.getResourceAsStream(path);
    }

}
