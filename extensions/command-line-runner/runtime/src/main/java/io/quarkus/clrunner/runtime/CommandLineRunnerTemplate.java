package io.quarkus.clrunner.runtime;

import java.util.Set;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.clrunner.CommandLineRunner;
import io.quarkus.runtime.MainArgsSupplier;
import io.quarkus.runtime.annotations.Template;

@Template
public class CommandLineRunnerTemplate {

    public void run(Set<Class<? extends CommandLineRunner>> handlerClasses, BeanContainer beanContainer,
            MainArgsSupplier mainArgsSupplier) {
        for (Class<? extends CommandLineRunner> handlerClass : handlerClasses) {
            final BeanContainer.Factory<? extends CommandLineRunner> factory = beanContainer.instanceFactory(handlerClass);
            final BeanContainer.Instance<? extends CommandLineRunner> instance = factory.create();
            final CommandLineRunner commandLineRunner = instance.get();

            commandLineRunner.run(mainArgsSupplier.getArgs());

            instance.close();
        }
    }
}
