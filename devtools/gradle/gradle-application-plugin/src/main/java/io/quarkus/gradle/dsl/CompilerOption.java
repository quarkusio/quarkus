package io.quarkus.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

public class CompilerOption {

    private final String name;
    private final List<String> opts = new ArrayList<>(0);

    public CompilerOption(String name) {
        this.name = name;
    }

    public CompilerOption args(List<String> options) {
        opts.addAll(options);
        return this;
    }

    public String getName() {
        return name;
    }

    public List<String> getArgs() {
        return opts;
    }

}
