package io.quarkus.resteasy.reactive.server.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.server.core.RuntimeExceptionMapper;

public class ExceptionMappersSupplier implements Supplier<List<ExceptionMappersSupplier.Entry>> {

    @Override
    public List<ExceptionMappersSupplier.Entry> get() {
        var mappers = RuntimeExceptionMapper.getMappers();
        var result = new ArrayList<Entry>(mappers.size());
        for (var entry : mappers.entrySet()) {
            result.add(new Entry(entry.getKey().getName(), entry.getValue().getClassName(), entry.getValue().getPriority()));
        }
        return result;
    }

    public static class Entry {
        private final String exceptionClassName;
        private final String mapperClassName;
        private final Integer priority;

        public Entry(String exceptionClassName, String mapperClassName, Integer priority) {
            this.exceptionClassName = exceptionClassName;
            this.mapperClassName = mapperClassName;
            this.priority = priority;
        }

        public String getExceptionClassName() {
            return exceptionClassName;
        }

        public String getMapperClassName() {
            return mapperClassName;
        }

        public Integer getPriority() {
            return priority;
        }
    }
}
