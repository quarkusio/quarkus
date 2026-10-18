package io.quarkus.maven.components;

import java.util.ArrayList;
import java.util.List;

public class CompilerOptions {

    private String name = null;
    private List<String> args = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setArgs(List<String> options) {
        this.args = options;
    }

    public List<String> getArgs() {
        return args;
    }

}
