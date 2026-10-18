package io.quarkus.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

public class CompilerOptions {

    private final List<CompilerOption> compilerOptions = new ArrayList<>(1);

    public CompilerOption compiler(String name) {
        CompilerOption compilerOption = new CompilerOption(name);
        compilerOptions.add(compilerOption);
        return compilerOption;
    }

    public List<CompilerOption> getCompilerOptions() {
        return compilerOptions;
    }

}
