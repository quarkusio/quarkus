package io.quarkus.cli;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {
    public static void main(String... args) {
        String old = System.getProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY);
        try {
            Quarkus.run(QuarkusCli.class, args);
        } finally {
            //needed to clean out the registry clients between tests
            QuarkusProjectHelper.reset();
            //TODO:it's not great we have to manually clear this
            if (old == null) {
                System.clearProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY);
            } else {
                System.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, old);
            }
        }
    }
}
