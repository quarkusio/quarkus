package io.quarkus.devtools.codestarts.core.strategy;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

public class CodestartFileStrategy implements Predicate<String> {

    private final String filter;
    private final CodestartFileStrategyHandler handler;

    public CodestartFileStrategy(String filter, CodestartFileStrategyHandler handler) {
        this.filter = requireNonNull(filter, "filter is required");
        this.handler = requireNonNull(handler, "handler is required");
    }

    public String getFilter() {
        return filter;
    }

    @Override
    public boolean test(String t) {
        if (t == null) {
            return false;
        }
        if (filter.startsWith("*") && filter.length() > 1) {
            if (t.endsWith(filter.substring(1))) {
                return true;
            }
        }
        if (filter.endsWith("*") && filter.length() > 1) {
            if (t.startsWith(filter.substring(0, filter.length() - 1))) {
                return true;
            }
        }
        return filter.equals(t);
    }

    public CodestartFileStrategyHandler getHandler() {
        return handler;
    }

    @Override
    public String toString() {
        return new StringBuilder(filter).append("->").append(handler.name()).toString();
    }
}
