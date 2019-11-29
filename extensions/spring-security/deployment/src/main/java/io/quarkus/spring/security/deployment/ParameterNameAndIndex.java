package io.quarkus.spring.security.deployment;

class ParameterNameAndIndex {
    private final int index;
    private final String name;

    public ParameterNameAndIndex(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }
}
