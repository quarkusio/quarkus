package io.quarkus.picocli.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import picocli.CommandLine;

class PicocliBeansFactory implements CommandLine.IFactory {
    private final CommandLine.IFactory defaultFactory = CommandLine.defaultFactory();

    @Override
    public <K> K create(Class<K> aClass) throws Exception {
        InstanceHandle<K> instance = Arc.container().instance(aClass);
        if (instance.isAvailable()) {
            return instance.get();
        }
        return defaultFactory.create(aClass);
    }
}
