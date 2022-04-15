package io.quarkus.resteasy.reactive.server.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ParamConverterProvidersSupplier implements Supplier<List<ParamConverterProvidersSupplier.Entry>> {

    @Override
    public List<ParamConverterProvidersSupplier.Entry> get() {
        var providers = ResteasyReactiveRecorder.getCurrentDeployment().getParamConverterProviders()
                .getParamConverterProviders();
        var result = new ArrayList<Entry>(providers.size());
        for (var provider : providers) {
            result.add(new Entry(provider.getClassName(), provider.getPriority()));
        }
        return result;
    }

    public static class Entry {
        private final String className;
        private final Integer priority;

        public Entry(String className, Integer priority) {
            this.className = className;
            this.priority = priority;
        }

        public String getClassName() {
            return className;
        }

        public Integer getPriority() {
            return priority;
        }
    }
}
