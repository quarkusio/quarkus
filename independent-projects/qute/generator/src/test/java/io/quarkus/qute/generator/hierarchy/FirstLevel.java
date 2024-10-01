package io.quarkus.qute.generator.hierarchy;

public interface FirstLevel {

    default int firstLevel() {
        return 1;
    }

}
