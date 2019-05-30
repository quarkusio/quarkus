package io.quarkus.arc.runtime;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Application;

@Unremovable
@ApplicationScoped
public class CommandLineParametersProducer {

    @Parameters
    @Produces
    @Dependent
    List<String> produceParameters() {
        Application application = Application.currentApplication();
        if (application == null) {
            // TODO investigate dev mode
            return Collections.emptyList();
        }

        return application.getCommandLineParameters();
    }
}
