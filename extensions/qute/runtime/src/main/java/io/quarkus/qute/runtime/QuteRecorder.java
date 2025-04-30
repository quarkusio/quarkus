package io.quarkus.qute.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuteRecorder {

    public Supplier<Object> createContext(List<String> templatePaths, List<String> tags, Map<String, List<String>> variants,
            Set<String> templateRoots, Map<String, String> templateContents, List<String> excludePatterns) {
        return new Supplier<Object>() {

            @Override
            public Object get() {
                return new QuteContext() {

                    volatile List<String> resolverClasses;
                    volatile List<String> templateGlobalProviderClasses;

                    @Override
                    public List<String> getTemplatePaths() {
                        return templatePaths;
                    }

                    @Override
                    public List<String> getTags() {
                        return tags;
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
                    public Map<String, String> getTemplateContents() {
                        return templateContents;
                    }

                    @Override
                    public List<String> getExcludePatterns() {
                        return excludePatterns;
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

    public void initializeGeneratedClasses(List<String> resolverClasses, List<String> templateGlobalProviderClasses) {
        QuteContext context = Arc.container().instance(QuteContext.class).get();
        context.setGeneratedClasses(resolverClasses, templateGlobalProviderClasses);
    }

    public interface QuteContext {

        List<String> getResolverClasses();

        List<String> getTemplatePaths();

        List<String> getTags();

        Map<String, List<String>> getVariants();

        List<String> getTemplateGlobalProviderClasses();

        Set<String> getTemplateRoots();

        Map<String, String> getTemplateContents();

        List<String> getExcludePatterns();

        /**
         * The generated classes must be initialized after the template expressions are validated (later during the STATIC_INIT
         * bootstrap phase) in order to break the cycle in the build chain.
         *
         * @param resolverClasses
         * @param templateGlobalProviderClasses
         */
        void setGeneratedClasses(List<String> resolverClasses, List<String> templateGlobalProviderClasses);

    }

}
