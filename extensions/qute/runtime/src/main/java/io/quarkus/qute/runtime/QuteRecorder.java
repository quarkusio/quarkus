package io.quarkus.qute.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuteRecorder {

    public Supplier<Object> createContext(List<String> resolverClasses,
            List<String> templatePaths, List<String> tags, Map<String, List<String>> variants,
            List<String> templateGlobalProviderClasses, Set<String> templateRoots) {
        return new Supplier<Object>() {

            @Override
            public Object get() {
                return new QuteContext() {

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
                        return resolverClasses;
                    }

                    @Override
                    public Map<String, List<String>> getVariants() {
                        return variants;
                    }

                    @Override
                    public List<String> getTemplateGlobalProviderClasses() {
                        return templateGlobalProviderClasses;
                    }

                    @Override
                    public Set<String> getTemplateRoots() {
                        return templateRoots;
                    }
                };
            }
        };
    }

    public interface QuteContext {

        List<String> getResolverClasses();

        List<String> getTemplatePaths();

        List<String> getTags();

        Map<String, List<String>> getVariants();

        List<String> getTemplateGlobalProviderClasses();

        Set<String> getTemplateRoots();

    }

}
