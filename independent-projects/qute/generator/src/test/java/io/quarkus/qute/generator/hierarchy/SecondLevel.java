package io.quarkus.qute.generator.hierarchy;

public interface SecondLevel {

    default int secondLevel() {
        return 2;
    }

}
