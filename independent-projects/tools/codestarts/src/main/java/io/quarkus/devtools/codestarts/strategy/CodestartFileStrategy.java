package io.quarkus.devtools.codestarts.strategy;

import java.util.Objects;
import java.util.function.Predicate;

public class CodestartFileStrategy implements Predicate<String> {

    private final String filter;
    private final CodestartFileStrategyHandler handler;

    public CodestartFileStrategy(String filter, CodestartFileStrategyHandler handler) {
        this.filter = filter;
        this.handler = handler;
    }

    @Override
    public boolean test(String t) {
        if (Objects.equals(filter, t)) {
            return true;
        }
        if (Objects.equals("*", t)) {
            return true;
        }
        // TODO SUPPORT FOR GLOB
        return false;
    }

    public CodestartFileStrategyHandler getHandler() {
        return handler;
    }
}
