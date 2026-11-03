package io.quarkus.qute.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.qute.ParserConfig;
import io.quarkus.runtime.annotations.RecordableConstructor;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuteRecorder {

    /**
     * Create the supplier of the Qute context. The template data is read here, at static init, so that a native
     * image holds it in the image heap and does not need the resource at run time.
     *
     * @param templateRoots the template roots
     * @param excludePatterns the template path exclude patterns
     * @return the supplier of the Qute context
     */
    public Supplier<Object> createContext(Set<String> templateRoots, List<String> excludePatterns) {
        QuteContextData data = readData();
        Map<String, TemplateInfo> templates = data.templates();
        Map<String, List<String>> variants = data.variants();
        Map<ParserConfig, Set<String>> nonDefaultParserConfigs = data.nonDefaultParserConfigs();
        return new Supplier<Object>() {

            @Override
            public Object get() {
                return new QuteContext() {

                    volatile List<String> resolverClasses;
                    volatile List<String> templateGlobalProviderClasses;

                    @Override
                    public Map<String, TemplateInfo> getTemplates() {
                        return templates;
                    }

                    @Override
                    public List<String> getResolverClasses() {
                        if (resolverClasses == null) {
                            throw generatedClassesNotInitialized();
                        }
                        return resolverClasses;
                    }

                    @Override
                    public Map<String, List<String>> getVariants() {
                        return variants;
                    }

                    @Override
                    public List<String> getTemplateGlobalProviderClasses() {
                        if (templateGlobalProviderClasses == null) {
                            throw generatedClassesNotInitialized();
                        }
                        return templateGlobalProviderClasses;
                    }

                    @Override
                    public Set<String> getTemplateRoots() {
                        return templateRoots;
                    }

                    @Override
                    public List<String> getExcludePatterns() {
                        return excludePatterns;
                    }

                    @Override
                    public Map<ParserConfig, Set<String>> getNonDefaultParserConfigs() {
                        return nonDefaultParserConfigs;
                    }

                    @Override
                    public void setGeneratedClasses(List<String> resolverClasses, List<String> templateGlobalProviderClasses) {
                        this.resolverClasses = resolverClasses;
                        this.templateGlobalProviderClasses = templateGlobalProviderClasses;
                    }

                    private IllegalStateException generatedClassesNotInitialized() {
                        return new IllegalStateException("Generated classes not initialized yet!");
                    }

                };
            }
        };
    }

    /**
     * Read the template data of the application. Only one copy of the resource may be visible, so that a
     * dependency that contains its own copy cannot replace the data of the application.
     */
    private static QuteContextData readData() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = QuteRecorder.class.getClassLoader();
        }
        try {
            List<URL> copies = Collections.list(cl.getResources(QuteContextData.RESOURCE));
            if (copies.isEmpty()) {
                throw new IllegalStateException("Qute template data not found: " + QuteContextData.RESOURCE);
            }
            if (copies.size() > 1) {
                throw new IllegalStateException("Only one copy of " + QuteContextData.RESOURCE
                        + " may be visible, but there are " + copies.size() + ": " + copies);
            }
            try (InputStream in = copies.get(0).openStream()) {
                return QuteContextData.read(in.readAllBytes());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void initializeGeneratedClasses(List<String> resolverClasses, List<String> templateGlobalProviderClasses) {
        QuteContext context = Arc.container().instance(QuteContext.class).get();
        context.setGeneratedClasses(resolverClasses, templateGlobalProviderClasses);
    }

    public interface QuteContext {

        List<String> getResolverClasses();

        Map<String, TemplateInfo> getTemplates();

        Map<String, List<String>> getVariants();

        List<String> getTemplateGlobalProviderClasses();

        Set<String> getTemplateRoots();

        List<String> getExcludePatterns();

        /**
         * @return the map of non-default parser configs to template ids
         */
        Map<ParserConfig, Set<String>> getNonDefaultParserConfigs();

        /**
         * The generated classes must be initialized after the template expressions are validated (later during the STATIC_INIT
         * bootstrap phase) in order to break the cycle in the build chain.
         *
         * @param resolverClasses
         * @param templateGlobalProviderClasses
         */
        void setGeneratedClasses(List<String> resolverClasses, List<String> templateGlobalProviderClasses);

        default List<String> getTags() {
            List<String> ret = new ArrayList<>();
            for (TemplateInfo template : getTemplates().values()) {
                if (template.isTag()) {
                    ret.add(template.path.substring(EngineProducer.TAGS.length(), template.path.length()));
                }
            }
            return ret;
        }

    }

    public static class TemplateInfo {

        public final String path;
        public final String source;
        public final String content;

        @RecordableConstructor
        public TemplateInfo(String path, String source, String content) {
            this.path = path;
            this.source = source;
            this.content = content;
        }

        public boolean isTag() {
            return path.startsWith(EngineProducer.TAGS);
        }

        public boolean hasContent() {
            return content != null;
        }

        public URI parseSource() {
            if (source != null) {
                try {
                    return new URI(source);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "TemplateInfo [path=" + path + ", source=" + source + "]";
        }

    }

}
