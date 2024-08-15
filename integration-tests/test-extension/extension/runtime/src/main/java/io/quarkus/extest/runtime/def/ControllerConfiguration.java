package io.quarkus.extest.runtime.def;

public interface ControllerConfiguration extends ResourceConfiguration {

    default String getName() {
        return "name";
    }
}
